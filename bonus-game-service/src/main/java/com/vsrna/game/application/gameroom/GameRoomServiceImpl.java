package com.vsrna.game.application.gameroom;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.gameevent.GameEventLogService;
import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.port.GameSchedulerPort;
import com.vsrna.game.application.round.RoundConstants;
import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.domain.barrel.*;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.exception.GameErrorMessages;
import com.vsrna.game.domain.gameroom.*;
import com.vsrna.game.domain.participant.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
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

    @Override
    @Transactional
    public GameRoomDetails createRoom(CreateGameRoomCommand command) {
        boolean isScheduled = command.scheduledStartAt() != null;

        GameRoom roomTemplate = new GameRoom(command.createdByUserId(), BigDecimal.ZERO);
        if (isScheduled) {
            roomTemplate.setStatus(GameRoomStatus.SCHEDULED);
        }
        GameRoom createdRoom = gameRoomRepository.create(roomTemplate);

        GameRoomConfig config = new GameRoomConfig(
                createdRoom.getId(),
                command.maxPlayers(),
                command.entryFeeAmount(),
                command.winnerPayoutPercentage(),
                command.boostCostAmount(),
                command.isBoostEnabled(),
                command.maxBarrelSelection(),
                command.scheduledStartAt(),
                command.repeatInterval()
        );
        gameRoomConfigRepository.create(config);

        List<Barrel> barrels = IntStream.rangeClosed(1, RoundConstants.BARRELS_PER_ROUND)
                .boxed()
                .flatMap(barrelNumber -> Stream.of(
                        new Barrel(createdRoom.getId(), 1, String.format("R1B%02d", barrelNumber), barrelNumber),
                        new Barrel(createdRoom.getId(), 2, String.format("R2B%02d", barrelNumber), barrelNumber)
                ))
                .toList();
        barrelRepository.createAll(barrels);

        if (isScheduled) {
            schedulerPort.scheduleRoomOpen(createdRoom.getId(), command.scheduledStartAt());
            notifierPort.publishRoomsUpdate(Map.of(
                    "type", "ROOM_SCHEDULED",
                    "roomId", createdRoom.getId().toString()
            ));
            gameEventLogService.log(createdRoom.getId(), "ROOM_SCHEDULED",
                    "scheduledAt=" + command.scheduledStartAt());
        } else {
            notifierPort.publishRoomsUpdate(Map.of(
                    "type", "ROOM_CREATED",
                    "roomId", createdRoom.getId().toString()
            ));
            gameEventLogService.log(createdRoom.getId(), "ROOM_CREATED",
                    "entryFee=" + command.entryFeeAmount() + " maxPlayers=" + command.maxPlayers());
        }

        return new GameRoomDetails(createdRoom, config);
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
                "type", "ROOM_CREATED",
                "roomId", roomId.toString()
        ));

        log.info("openScheduledRoom: room {} is now WAITING", roomId);

        if (config.getRepeatInterval() != null && config.getScheduledStartAt() != null) {
            Instant nextStartAt = config.getScheduledStartAt();
            Instant now = Instant.now();
            do {
                nextStartAt = config.getRepeatInterval().next(nextStartAt);
            } while (!nextStartAt.isAfter(now));
            CreateGameRoomCommand nextCommand = new CreateGameRoomCommand(
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
            GameRoomDetails nextRoom = createRoom(nextCommand);
            log.info("openScheduledRoom: created next recurring room {} at {}", nextRoom.room().getId(), nextStartAt);
        }
    }

    @Override
    @Transactional
    public GameRoomDetails joinRoom(UUID roomId, UUID userId, String displayName) {
        // Check balance BEFORE acquiring the DB lock — keeps lock window short (no HTTP inside transaction)
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
                    .map(d -> Map.<String, Object>of(
                            "roomId", d.room().getId().toString(),
                            "entryFee", d.config().getEntryFeeAmount()))
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

        // Write balance reserve command and notification atomically with participant creation
        gameEventPort.publishBalanceReserve(userId, config.getEntryFeeAmount(), roomId);
        gameEventPort.publishEntryReserved(userId, roomId, config.getEntryFeeAmount());
        gameEventLogService.log(roomId, "PLAYER_JOINED", "userId=" + userId);

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

        Instant expiresAt = waitTimerExpiresAt != null
                ? waitTimerExpiresAt
                : room.getWaitTimerExpiresAt();
        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("type", "ROOM_UPDATED");
        roomUpdate.put("currentPlayers", newCount);
        roomUpdate.put("prizePool", newPrize);
        roomUpdate.put("winProbability", newCount > 0 ? 1.0 / newCount : 1.0);
        if (expiresAt != null) {
            roomUpdate.put("waitExpiresAt", expiresAt.toEpochMilli());
        }
        notifierPort.publishRoomUpdate(roomId, roomUpdate);

        return new GameRoomDetails(room, config);
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
                    "type", "ROOM_STARTED",
                    "roomId", roomId.toString()
            ));
            gameEventLogService.log(roomId, "ROOM_STARTED", "totalPlayers=" + total);
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
        return new GameRoomDetails(room, config);
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
                ? targetEntryFee.multiply(BigDecimal.valueOf(0.8))
                : null;
        BigDecimal feeMax = targetEntryFee != null
                ? targetEntryFee.multiply(BigDecimal.valueOf(1.2))
                : null;
        List<GameRoomDetails> rooms = listRooms(
                GameRoomQuery.filtered(GameRoomStatus.WAITING, feeMin, feeMax, targetMaxPlayers, true, 0, 1));
        if (rooms.isEmpty()) {
            // Расширяем поиск — без фильтра по maxPlayers
            rooms = listRooms(
                    GameRoomQuery.filtered(GameRoomStatus.WAITING, feeMin, feeMax, null, true, 0, 1));
        }
        return rooms.stream()
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("GameRoom", GameErrorMessages.ROOM_NO_SUITABLE_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NextGameOption> nextGame(UUID finishedRoomId) {
        GameRoomDetails finished = getRoom(finishedRoomId);
        GameRoomConfig cfg = finished.config();
        BigDecimal fee = cfg.getEntryFeeAmount();
        int players = cfg.getMaxPlayers();

        List<NextGameOption> options = new ArrayList<>();

        // SAME — та же конфигурация
        listRooms(GameRoomQuery.filtered(
                GameRoomStatus.WAITING,
                fee.multiply(BigDecimal.valueOf(0.9)),
                fee.multiply(BigDecimal.valueOf(1.1)),
                players, true, 0, 1))
                .stream().findFirst()
                .ifPresent(r -> options.add(new NextGameOption("SAME", r)));

        // SAFER — вдвое дешевле или на меньше игроков
        BigDecimal saferFee = fee.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        listRooms(GameRoomQuery.filtered(
                GameRoomStatus.WAITING, BigDecimal.ZERO, saferFee, null, true, 0, 1))
                .stream().findFirst()
                .ifPresent(r -> options.add(new NextGameOption("SAFER", r)));

        // RISKIER — вдвое дороже
        BigDecimal riskierFeeMin = fee.multiply(BigDecimal.valueOf(1.5));
        listRooms(GameRoomQuery.filtered(
                GameRoomStatus.WAITING, riskierFeeMin, null, null, true, 0, 1))
                .stream().findFirst()
                .ifPresent(r -> options.add(new NextGameOption("RISKIER", r)));

        return options;
    }

    @Override
    @Transactional
    public void cancelRoom(UUID roomId, UUID adminUserId) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        if (room.getStatus() != GameRoomStatus.WAITING) {
            throw ApiException.badRequest(GameErrorMessages.ROOM_ONLY_WAITING_CAN_CANCEL);
        }
        schedulerPort.cancel(roomId, "fill-bots");

        // Release reserved balance for all real participants via Kafka (outbox, same transaction)
        List<GameParticipant> participants = participantRepository.list(GameParticipantQuery.byRoom(roomId));
        for (GameParticipant participant : participants) {
            if (participant.isRealPlayer()) {
                gameEventPort.publishBalanceRelease(participant.getUserId(), participant.getReservedPoints(), roomId);
            }
        }

        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.finished(Instant.now()));

        notifierPort.publishRoomsUpdate(Map.of(
                "type", "ROOM_CANCELLED",
                "roomId", roomId.toString()
        ));
        gameEventLogService.log(roomId, "ROOM_CANCELLED", "cancelledBy=" + adminUserId);

        log.info("Room {} cancelled by admin {}", roomId, adminUserId);
    }

    private List<GameRoomDetails> buildRoomDetails(List<GameRoom> rooms) {
        if (rooms.isEmpty()) return List.of();
        List<UUID> roomIds = rooms.stream().map(GameRoom::getId).toList();
        List<GameRoomConfig> configs = gameRoomConfigRepository.listByRoomIds(roomIds);
        Map<UUID, GameRoomConfig> configByRoomId = new HashMap<>();
        configs.forEach(config -> configByRoomId.put(config.getGameRoomId(), config));
        return rooms.stream()
                .filter(room -> configByRoomId.containsKey(room.getId()))
                .map(room -> new GameRoomDetails(room, configByRoomId.get(room.getId())))
                .toList();
    }

    @Override
    public ConfigEvaluationResult evaluateConfig(CreateGameRoomCommand command) {
        return configValidator.evaluate(
                command.maxPlayers(),
                command.entryFeeAmount(),
                command.winnerPayoutPercentage(),
                command.boostCostAmount(),
                command.isBoostEnabled(),
                command.maxBarrelSelection()
        );
    }

}
