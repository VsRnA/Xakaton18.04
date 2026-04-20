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

        balancePort.reserve(userId, config.getEntryFeeAmount(), roomId);

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
        List<GameRoom> rooms = gameRoomRepository.list(new GameRoomQuery(null, status, null, page, size));
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
}
