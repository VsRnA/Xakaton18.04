package com.vsrna.game.application.gameroom;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.port.GameSchedulerPort;
import com.vsrna.game.application.round.RoundConstants;
import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.domain.barrel.*;
import com.vsrna.game.domain.exception.ApiException;
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
    private final BotService botService;
    private final GameSchedulerPort schedulerPort;
    private final GameNotifierPort notifierPort;
    private final RoundService roundService;
    private final GameRoomConfigValidator configValidator;

    @Override
    @Transactional
    public GameRoomDetails createRoom(CreateGameRoomCommand command) {
        GameRoom room = new GameRoom(command.createdByUserId(), BigDecimal.ZERO);
        room = gameRoomRepository.create(room);

        GameRoomConfig config = new GameRoomConfig(
                room.getId(),
                command.maxPlayers(),
                command.entryFeeAmount(),
                command.winnerPayoutPercentage(),
                command.boostCostAmount(),
                command.isBoostEnabled(),
                command.maxBarrelSelection()
        );
        gameRoomConfigRepository.create(config);

        List<Barrel> barrels = new ArrayList<>();
        for (int i = 1; i <= RoundConstants.BARRELS_PER_ROUND; i++) {
            barrels.add(new Barrel(room.getId(), 1, String.format("R1B%02d", i), i));
        }
        for (int i = 1; i <= RoundConstants.BARRELS_PER_ROUND; i++) {
            barrels.add(new Barrel(room.getId(), 2, String.format("R2B%02d", i), i));
        }
        barrelRepository.createAll(barrels);

        notifierPort.publishRoomsUpdate(Map.of(
                "type", "ROOM_CREATED",
                "roomId", room.getId().toString()
        ));

        return new GameRoomDetails(room, config);
    }

    @Override
    @Transactional
    public GameRoomDetails joinRoom(UUID roomId, UUID userId, String displayName) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        if (room.getStatus() != GameRoomStatus.WAITING) {
            throw ApiException.badRequest("Room is not accepting players");
        }
        if (room.getCurrentPlayerCount() >= config.getMaxPlayers()) {
            throw ApiException.badRequest("Room is full");
        }

        GameParticipant participant = new GameParticipant(roomId, userId, false, displayName, config.getEntryFeeAmount());
        try {
            participantRepository.create(participant);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.alreadyExists("GameParticipant", "User already joined this room");
        }

        int newCount = room.getCurrentPlayerCount() + 1;
        BigDecimal newPrize = room.getPrizePoolAmount().add(config.getEntryFeeAmount());
        room = gameRoomRepository.update(
                GameRoomQuery.byId(roomId),
                new GameRoomPatch(null, newCount, newPrize, null, null, null)
        );

        try {
            balancePort.reserve(userId, config.getEntryFeeAmount(), roomId);
        } catch (Exception e) {
            // Spring откатит транзакцию при re-throw. Ищем альтернативы дешевле.
            List<GameRoomDetails> alternatives = listRoomsFiltered(
                    GameRoomQuery.filtered(GameRoomStatus.WAITING, BigDecimal.ZERO,
                            config.getEntryFeeAmount().subtract(BigDecimal.ONE),
                            null, true, 0, 3));
            List<Map<String, Object>> altList = alternatives.stream()
                    .map(d -> Map.<String, Object>of(
                            "roomId", d.room().getId().toString(),
                            "entryFee", d.config().getEntryFeeAmount()))
                    .toList();
            throw ApiException.insufficientBalance(
                    "Недостаточно бонусных баллов для входа в комнату. Требуется: "
                            + config.getEntryFeeAmount(),
                    Map.of("required", config.getEntryFeeAmount(), "suggestedRooms", altList));
        }

        Instant waitTimerExpiresAt = null;
        if (newCount == 1) {
            waitTimerExpiresAt = schedulerPort.scheduleWaitTimerExpiry(roomId);
            gameRoomRepository.update(
                    GameRoomQuery.byId(roomId),
                    new GameRoomPatch(null, null, null, null, null, waitTimerExpiresAt)
            );
        } else if (newCount >= config.getMaxPlayers()) {
            schedulerPort.cancel(roomId, "fill-bots");
            roundService.startRound(roomId, 1);
            notifierPort.publishRoomsUpdate(Map.of(
                    "type", "ROOM_FULL",
                    "roomId", roomId.toString()
            ));
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
        } else {
            log.warn("fillWithBots: not enough participants ({}) to start room {}", total, roomId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameRoomDetails> listRooms(GameRoomStatus status, int page, int size) {
        List<GameRoom> rooms = gameRoomRepository.list(new GameRoomQuery(null, status, null, page, size, null, null, null, null));
        if (rooms.isEmpty()) return List.of();
        List<UUID> roomIds = rooms.stream().map(GameRoom::getId).toList();
        List<GameRoomConfig> configs = gameRoomConfigRepository.listByRoomIds(roomIds);
        java.util.Map<UUID, GameRoomConfig> configByRoomId = new java.util.HashMap<>();
        configs.forEach(c -> configByRoomId.put(c.getGameRoomId(), c));
        return rooms.stream()
                .map(r -> new GameRoomDetails(r, configByRoomId.get(r.getId())))
                .toList();
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
    public List<GameRoomDetails> listRoomsFiltered(GameRoomQuery query) {
        List<GameRoom> rooms = gameRoomRepository.list(query);
        if (rooms.isEmpty()) return List.of();
        List<UUID> roomIds = rooms.stream().map(GameRoom::getId).toList();
        List<GameRoomConfig> configs = gameRoomConfigRepository.listByRoomIds(roomIds);
        Map<UUID, GameRoomConfig> configByRoomId = new HashMap<>();
        configs.forEach(c -> configByRoomId.put(c.getGameRoomId(), c));
        return rooms.stream()
                .filter(r -> configByRoomId.containsKey(r.getId()))
                .map(r -> new GameRoomDetails(r, configByRoomId.get(r.getId())))
                .toList();
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
        List<GameRoomDetails> rooms = listRoomsFiltered(
                GameRoomQuery.filtered(GameRoomStatus.WAITING, feeMin, feeMax, targetMaxPlayers, true, 0, 1));
        if (rooms.isEmpty()) {
            // Расширяем поиск — без фильтра по maxPlayers
            rooms = listRoomsFiltered(
                    GameRoomQuery.filtered(GameRoomStatus.WAITING, feeMin, feeMax, null, true, 0, 1));
        }
        if (rooms.isEmpty()) {
            throw ApiException.notFound("GameRoom", "No suitable WAITING room found for the given parameters");
        }
        return rooms.get(0);
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
        listRoomsFiltered(GameRoomQuery.filtered(
                GameRoomStatus.WAITING,
                fee.multiply(BigDecimal.valueOf(0.9)),
                fee.multiply(BigDecimal.valueOf(1.1)),
                players, true, 0, 1))
                .stream().findFirst()
                .ifPresent(r -> options.add(new NextGameOption("SAME", r)));

        // SAFER — вдвое дешевле или на меньше игроков
        BigDecimal saferFee = fee.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        listRoomsFiltered(GameRoomQuery.filtered(
                GameRoomStatus.WAITING, BigDecimal.ZERO, saferFee, null, true, 0, 1))
                .stream().findFirst()
                .ifPresent(r -> options.add(new NextGameOption("SAFER", r)));

        // RISKIER — вдвое дороже
        BigDecimal riskierFeeMin = fee.multiply(BigDecimal.valueOf(1.5));
        listRoomsFiltered(GameRoomQuery.filtered(
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
            throw ApiException.badRequest("Only WAITING rooms can be cancelled");
        }
        schedulerPort.cancel(roomId, "fill-bots");

        // Возвращаем баллы всем реальным участникам
        List<GameParticipant> participants = participantRepository.list(GameParticipantQuery.byRoom(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        for (GameParticipant p : participants) {
            if (p.isRealPlayer()) {
                try {
                    balancePort.release(p.getUserId(), config.getEntryFeeAmount(), roomId);
                } catch (Exception e) {
                    log.error("Failed to release balance for participant {} in cancelled room {}", p.getId(), roomId, e);
                }
            }
        }

        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.finished(Instant.now()));

        notifierPort.publishRoomsUpdate(Map.of(
                "type", "ROOM_CANCELLED",
                "roomId", roomId.toString()
        ));

        log.info("Room {} cancelled by admin {}", roomId, adminUserId);
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
