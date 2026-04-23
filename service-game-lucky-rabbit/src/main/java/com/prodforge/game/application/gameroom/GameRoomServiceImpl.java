package com.prodforge.game.application.gameroom;

import com.prodforge.game.application.bot.BotService;
import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.port.BalancePort;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.application.port.GameEventTypes;
import com.prodforge.game.application.port.GameNotifierPort;
import com.prodforge.game.application.port.GamePhase;
import com.prodforge.game.application.port.GameSchedulerPort;
import com.prodforge.game.application.gameroom.config.ConfigEvaluationResult;
import com.prodforge.game.application.gameroom.config.GameRoomConfigValidator;
import com.prodforge.game.application.gameroom.config.GameRoomConstants;
import com.prodforge.game.application.metrics.GameMetrics;
import com.prodforge.game.application.round.RoundService;
import com.prodforge.game.application.round.scoring.RoundConstants;
import com.prodforge.game.domain.barrel.*;
import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.exception.GameErrorMessages;
import com.prodforge.game.domain.gameroom.*;
import com.prodforge.game.domain.participant.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {

    private static final int MIN_PLAYERS_TO_START = 2;

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final BalancePort balancePort;
    private final GameEventPort gameEventPort;
    private final BotService botService;
    private final GameSchedulerPort schedulerPort;
    private final GameNotifierPort notifierPort;
    private final RoundService roundService;
    private final GameRoomConfigValidator configValidator;
    private final GameEventLogService gameEventLogService;
    private final GameMetrics gameMetrics;

    @Override
    @Transactional
    public GameRoomDetails createRoom(CreateGameRoomRequest request) {
        boolean isScheduled = request.scheduledStartAt() != null;

        GameRoom roomTemplate = new GameRoom(request.createdByUserId(), BigDecimal.ZERO);
        if (isScheduled) {
            roomTemplate.setStatus(GameRoomStatus.SCHEDULED);
        }
        GameRoom createdRoom = gameRoomRepository.create(roomTemplate);

        GameRoomConfig config = new GameRoomConfig(
                createdRoom.getId(),
                request.maxPlayers(),
                request.entryFeeAmount(),
                request.winnerPayoutPercentage(),
                request.boostCostAmount(),
                request.isBoostEnabled(),
                request.maxBarrelSelection(),
                request.scheduledStartAt(),
                request.repeatInterval()
        );
        gameRoomConfigRepository.create(config);

        List<Barrel> barrels = IntStream.rangeClosed(1, RoundConstants.BARRELS_PER_ROUND)
                .boxed()
                .flatMap(barrelNumber -> Stream.of(
                        new Barrel(createdRoom.getId(), 1,
                                String.format(GameRoomConstants.BARREL_NAME_FORMAT, 1, barrelNumber), barrelNumber),
                        new Barrel(createdRoom.getId(), 2,
                                String.format(GameRoomConstants.BARREL_NAME_FORMAT, 2, barrelNumber), barrelNumber)
                ))
                .toList();
        barrelRepository.createAll(barrels);

        if (isScheduled) {
            schedulerPort.scheduleRoomOpen(createdRoom.getId(), request.scheduledStartAt());
            notifierPort.publishRoomsUpdate(Map.of(
                    GameEventTypes.FIELD_TYPE, GameEventTypes.ROOM_SCHEDULED,
                    "roomId", createdRoom.getId().toString()
            ));
            gameEventLogService.log(createdRoom.getId(), GameEventTypes.ROOM_SCHEDULED,
                    Map.of("scheduledAt", request.scheduledStartAt().toString()));
            gameMetrics.roomsCreatedScheduled.increment();
        } else {
            notifierPort.publishRoomsUpdate(Map.of(
                    GameEventTypes.FIELD_TYPE, GameEventTypes.ROOM_CREATED,
                    "roomId", createdRoom.getId().toString()
            ));
            gameEventLogService.log(createdRoom.getId(), GameEventTypes.ROOM_CREATED,
                    Map.of("entryFee", request.entryFeeAmount(), "maxPlayers", request.maxPlayers()));
            gameMetrics.roomsCreatedImmediate.increment();
        }

        return new GameRoomDetails(createdRoom, config, List.of());
    }

    @Override
    @Transactional
    public void openScheduledRoom(UUID roomId) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        if (room.getStatus() != GameRoomStatus.SCHEDULED) {
            log.warn("openScheduledRoom: room {} is not SCHEDULED ({}), skipping", roomId, room.getStatus());
            return;
        }
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                new GameRoomPatch(GameRoomStatus.WAITING, null, null, null, null, null));

        notifierPort.publishRoomsUpdate(Map.of(
                GameEventTypes.FIELD_TYPE, GameEventTypes.ROOM_CREATED,
                "roomId", roomId.toString()
        ));

        log.info("openScheduledRoom: room {} is now WAITING", roomId);

        if (config.getRepeatInterval() != null && config.getScheduledStartAt() != null) {
            Instant nextStartAt = config.getScheduledStartAt();
            Instant now = Instant.now();
            do {
                nextStartAt = config.getRepeatInterval().next(nextStartAt);
            } while (!nextStartAt.isAfter(now));
            CreateGameRoomRequest nextRequest = new CreateGameRoomRequest(
                    room.getCreatedByUserId(),
                    config.getMaxPlayers(),
                    config.getEntryFeeAmount(),
                    config.getWinnerPayoutPercentage(),
                    config.getBoostCostAmount(),
                    config.isBoostEnabled(),
                    config.getMaxBarrelSelection(),
                    nextStartAt,
                    config.getRepeatInterval()
            );
            GameRoomDetails nextRoom = createRoom(nextRequest);
            log.info("openScheduledRoom: created next recurring room {} at {}", nextRoom.room().getId(), nextStartAt);
        }
    }

    @Override
    @Transactional
    public GameRoomDetails joinRoom(UUID roomId, UUID userId, String displayName) {
        BigDecimal available = balancePort.getAvailableBalance(userId);

        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        if (room.getStatus() != GameRoomStatus.WAITING) {
            throw ApiException.badRequest(GameErrorMessages.ROOM_NOT_ACCEPTING);
        }
        if (room.getCurrentPlayerCount() >= config.getMaxPlayers()) {
            throw ApiException.badRequest(GameErrorMessages.ROOM_FULL);
        }
        if (available.compareTo(config.getEntryFeeAmount()) < 0) {
            List<GameRoomDetails> alternatives = listRooms(
                    GameRoomQuery.filtered(GameRoomStatus.WAITING, BigDecimal.ZERO,
                            config.getEntryFeeAmount().subtract(BigDecimal.ONE),
                            null, true, 0, 3));
            List<Map<String, Object>> altList = alternatives.stream()
                    .map(details -> Map.<String, Object>of(
                            "roomId", details.room().getId().toString(),
                            "entryFee", details.config().getEntryFeeAmount()))
                    .toList();
            throw ApiException.insufficientBalance(
                    GameErrorMessages.insufficientBalanceForEntry(config.getEntryFeeAmount()),
                    Map.of("required", config.getEntryFeeAmount(), "suggestedRooms", altList));
        }

        GameParticipant participant = new GameParticipant(roomId, userId, false, displayName, config.getEntryFeeAmount());
        try {
            participantRepository.create(participant);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.alreadyExists("GameParticipant", GameErrorMessages.ROOM_PARTICIPANT_ALREADY_JOINED);
        }

        gameEventPort.publishBalanceReserve(userId, config.getEntryFeeAmount(), roomId);
        gameEventPort.publishEntryReserved(userId, roomId, config.getEntryFeeAmount());
        gameEventLogService.log(roomId, GameEventTypes.PLAYER_JOINED, Map.of("userId", userId.toString()));
        gameMetrics.playersJoined.increment();

        int newCount = room.getCurrentPlayerCount() + 1;
        BigDecimal newPrize = room.getPrizePoolAmount().add(config.getEntryFeeAmount());
        room = gameRoomRepository.update(
                GameRoomQuery.byId(roomId),
                new GameRoomPatch(null, newCount, newPrize, null, null, null)
        );

        Instant waitTimerExpiresAt = null;
        if (newCount == 1) {
            waitTimerExpiresAt = schedulerPort.scheduleWaitTimerExpiry(roomId);
            gameRoomRepository.update(
                    GameRoomQuery.byId(roomId),
                    new GameRoomPatch(null, null, null, null, null, waitTimerExpiresAt)
            );
        }

        Instant expiresAt = waitTimerExpiresAt != null ? waitTimerExpiresAt : room.getWaitTimerExpiresAt();
        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put(GameEventTypes.FIELD_TYPE, GameEventTypes.ROOM_UPDATED);
        roomUpdate.put("currentPlayers", newCount);
        roomUpdate.put("prizePool", newPrize);
        roomUpdate.put("winProbability", newCount > 0 ? 1.0 / newCount : 1.0);
        if (expiresAt != null) {
            roomUpdate.put("waitExpiresAt", expiresAt.toEpochMilli());
        }
        notifierPort.publishRoomUpdate(roomId, roomUpdate);

        return new GameRoomDetails(room, config, List.of());
    }

    @Override
    @Transactional
    public void fillWithBots(UUID roomId) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        log.info("fillWithBots: room={} status={} players={}", roomId, room.getStatus(), room.getCurrentPlayerCount());
        if (room.getStatus() != GameRoomStatus.WAITING) {
            log.info("fillWithBots: room {} is not WAITING ({}), skipping", roomId, room.getStatus());
            return;
        }
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        int botCount = config.getMaxPlayers() - room.getCurrentPlayerCount();
        log.info("fillWithBots: adding {} bots to room {}", botCount, roomId);

        if (botCount > 0) {
            botService.createBotsForRoom(roomId, botCount, config.getEntryFeeAmount());
            int newCount = room.getCurrentPlayerCount() + botCount;
            BigDecimal botPrize = config.getEntryFeeAmount().multiply(BigDecimal.valueOf(botCount));
            BigDecimal newPrize = room.getPrizePoolAmount().add(botPrize);
            gameRoomRepository.update(GameRoomQuery.byId(roomId),
                    new GameRoomPatch(null, newCount, newPrize, null, null, null));
        }

        int total = participantRepository.count(GameParticipantQuery.byRoom(roomId));
        log.info("fillWithBots: total participants={} in room {}", total, roomId);
        if (total >= MIN_PLAYERS_TO_START) {
            log.info("fillWithBots: starting round 1 for room {}", roomId);
            roundService.startRound(roomId, 1);
            notifierPort.publishRoomsUpdate(Map.of(
                    GameEventTypes.FIELD_TYPE, GameEventTypes.ROOM_STARTED,
                    "roomId", roomId.toString()
            ));
            gameEventLogService.log(roomId, GameEventTypes.ROOM_STARTED, Map.of("totalPlayers", total));
            gameMetrics.roomsStarted.increment();
        } else {
            log.warn("fillWithBots: not enough participants ({}) to start room {}", total, roomId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameRoomDetails> listRooms(GameRoomQuery query) {
        return buildRoomDetails(gameRoomRepository.list(query));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameParticipant> listParticipants(UUID roomId) {
        gameRoomRepository.get(GameRoomQuery.byId(roomId));
        return participantRepository.list(GameParticipantQuery.byRoom(roomId));
    }

    @Override
    @Transactional(readOnly = true)
    public GameRoomDetails getRoom(UUID roomId) {
        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        List<GameParticipant> participants = participantRepository.list(GameParticipantQuery.byRoom(roomId));
        return new GameRoomDetails(room, config, participants);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameRoomDetails> affordableRooms(UUID userId, int page, int size) {
        BigDecimal balance = balancePort.getAvailableBalance(userId);
        return listRooms(GameRoomQuery.filtered(
                GameRoomStatus.WAITING, BigDecimal.ZERO, balance, null, true, page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public GameRoomDetails suggestRoom(BigDecimal targetEntryFee, Integer targetMaxPlayers) {
        BigDecimal feeMin = targetEntryFee != null
                ? targetEntryFee.multiply(GameRoomConstants.FEE_RANGE_MIN_FACTOR)
                : null;
        BigDecimal feeMax = targetEntryFee != null
                ? targetEntryFee.multiply(GameRoomConstants.FEE_RANGE_MAX_FACTOR)
                : null;
        List<GameRoomDetails> rooms = listRooms(
                GameRoomQuery.filtered(GameRoomStatus.WAITING, feeMin, feeMax, targetMaxPlayers, true, 0, 1));
        if (rooms.isEmpty()) {
            rooms = listRooms(
                    GameRoomQuery.filtered(GameRoomStatus.WAITING, feeMin, feeMax, null, true, 0, 1));
        }
        return rooms.stream()
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("GameRoom", GameErrorMessages.ROOM_NO_SUITABLE_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NextGameOption> nextGame(UUID finishedRoomId, UUID userId) {
        GameRoomDetails finished = getRoom(finishedRoomId);
        GameRoomConfig cfg = finished.config();
        BigDecimal fee = cfg.getEntryFeeAmount();
        int players = cfg.getMaxPlayers();
        BigDecimal balance = balancePort.getAvailableBalance(userId);

        List<NextGameOption> options = new ArrayList<>();

        if (balance.compareTo(fee.multiply(GameRoomConstants.SAME_FEE_MIN_FACTOR)) >= 0) {
            BigDecimal sameMax = fee.multiply(GameRoomConstants.SAME_FEE_MAX_FACTOR).min(balance);
            listRooms(GameRoomQuery.filtered(
                    GameRoomStatus.WAITING,
                    fee.multiply(GameRoomConstants.SAME_FEE_MIN_FACTOR),
                    sameMax,
                    players, true, 0, 1))
                    .stream().findFirst()
                    .ifPresent(room -> options.add(new NextGameOption(GameRoomConstants.NEXT_GAME_SAME, room)));
        }

        BigDecimal saferFeeMax = fee.divide(GameRoomConstants.SAFER_FEE_DIVISOR, 2, RoundingMode.HALF_UP).min(balance);
        if (saferFeeMax.compareTo(BigDecimal.ZERO) > 0) {
            listRooms(GameRoomQuery.filtered(
                    GameRoomStatus.WAITING, BigDecimal.ZERO, saferFeeMax, null, true, 0, 1))
                    .stream().findFirst()
                    .ifPresent(room -> options.add(new NextGameOption(GameRoomConstants.NEXT_GAME_SAFER, room)));
        }

        BigDecimal riskierFeeMin = fee.multiply(GameRoomConstants.RISKIER_FEE_MIN_FACTOR);
        if (balance.compareTo(riskierFeeMin) >= 0) {
            listRooms(GameRoomQuery.filtered(
                    GameRoomStatus.WAITING, riskierFeeMin, balance, null, true, 0, 1))
                    .stream().findFirst()
                    .ifPresent(room -> options.add(new NextGameOption(GameRoomConstants.NEXT_GAME_RISKIER, room)));
        }

        return options;
    }

    @Override
    @Transactional
    public void cancelRoom(UUID roomId, UUID adminUserId) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        if (room.getStatus() != GameRoomStatus.WAITING) {
            throw ApiException.badRequest(GameErrorMessages.ROOM_ONLY_WAITING_CAN_CANCEL);
        }
        schedulerPort.cancel(roomId, GamePhase.FILL_BOTS);

        List<GameParticipant> participants = participantRepository.list(GameParticipantQuery.byRoom(roomId));
        for (GameParticipant participant : participants) {
            if (participant.isRealPlayer()) {
                gameEventPort.publishBalanceRelease(participant.getUserId(), participant.getReservedPoints(), roomId);
            }
        }

        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.finished(Instant.now()));

        notifierPort.publishRoomsUpdate(Map.of(
                GameEventTypes.FIELD_TYPE, GameEventTypes.ROOM_CANCELLED,
                "roomId", roomId.toString()
        ));
        gameEventLogService.log(roomId, GameEventTypes.ROOM_CANCELLED, Map.of("cancelledBy", adminUserId.toString()));
        gameMetrics.roomsCancelled.increment();

        log.info("Room {} cancelled by admin {}", roomId, adminUserId);
    }

    private List<GameRoomDetails> buildRoomDetails(List<GameRoom> rooms) {
        if (rooms.isEmpty()) return List.of();
        List<UUID> roomIds = rooms.stream().map(GameRoom::getId).toList();
        List<GameRoomConfig> configs = gameRoomConfigRepository.listByRoomIds(roomIds);
        Map<UUID, GameRoomConfig> configByRoomId = new HashMap<>();
        configs.forEach(config -> configByRoomId.put(config.getGameRoomId(), config));
        Map<UUID, List<GameParticipant>> participantsByRoomId = new HashMap<>();
        participantRepository.listByRoomIds(roomIds)
                .forEach(participant -> participantsByRoomId
                        .computeIfAbsent(participant.getGameRoomId(), key -> new ArrayList<>())
                        .add(participant));
        return rooms.stream()
                .filter(room -> configByRoomId.containsKey(room.getId()))
                .map(room -> new GameRoomDetails(
                        room,
                        configByRoomId.get(room.getId()),
                        participantsByRoomId.getOrDefault(room.getId(), List.of())))
                .toList();
    }

    @Override
    public ConfigEvaluationResult evaluateConfig(CreateGameRoomRequest request) {
        return configValidator.evaluate(
                request.maxPlayers(),
                request.entryFeeAmount(),
                request.winnerPayoutPercentage(),
                request.boostCostAmount(),
                request.isBoostEnabled(),
                request.maxBarrelSelection()
        );
    }
}
