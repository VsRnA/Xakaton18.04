package com.vsrna.game.application.gameroom;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.port.GameSchedulerPort;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {

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
                command.isBoostEnabled()
        );
        gameRoomConfigRepository.create(config);

        List<Barrel> barrels = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            barrels.add(new Barrel(room.getId(), 1, String.format("R1B%02d", i), i));
        }
        for (int i = 1; i <= 10; i++) {
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
    public GameRoomDetails joinRoom(UUID roomId, UUID userId) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        if (room.getStatus() != GameRoomStatus.WAITING) {
            throw ApiException.badRequest("Room is not accepting players");
        }
        if (room.getCurrentPlayerCount() >= config.getMaxPlayers()) {
            throw ApiException.badRequest("Room is full");
        }

        GameParticipant participant = new GameParticipant(roomId, userId, false, config.getEntryFeeAmount());
        try {
            participantRepository.create(participant);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.alreadyExists("GameParticipant", "User already joined this room");
        }

        int newCount = room.getCurrentPlayerCount() + 1;
        BigDecimal newPrize = room.getPrizePoolAmount().add(config.getEntryFeeAmount());
        room = gameRoomRepository.update(
                GameRoomQuery.byId(roomId),
                new GameRoomPatch(null, newCount, newPrize, null, null)
        );

        if (newCount == 1) {
            schedulerPort.scheduleWaitTimerExpiry(roomId);
        } else if (newCount >= config.getMaxPlayers()) {
            schedulerPort.cancel(roomId, "fill-bots");
            roundService.startRound(roomId, 1);
            notifierPort.publishRoomsUpdate(Map.of(
                    "type", "ROOM_FULL",
                    "roomId", roomId.toString()
            ));
        }

        notifierPort.publishRoomUpdate(roomId, Map.of(
                "type", "ROOM_UPDATED",
                "currentPlayers", newCount,
                "prizePool", newPrize,
                "winProbability", newCount > 0 ? 1.0 / newCount : 1.0
        ));

        // HTTP-вызов выполняется после коммита транзакции:
        // если коммит провалится — резерв не будет вызван (деньги не заблокируются).
        // если HTTP провалится после успешного коммита — пользователь остаётся в комнате
        // без реального резерва: логируем для ручного разбора / компенсирующей транзакции.
        final UUID finalUserId = userId;
        final BigDecimal finalEntryFee = config.getEntryFeeAmount();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    balancePort.reserve(finalUserId, finalEntryFee, roomId);
                } catch (Exception e) {
                    log.error("COMPENSATION NEEDED: failed to reserve balance after commit " +
                              "userId={}, roomId={}: {}", finalUserId, roomId, e.getMessage());
                }
            }
        });

        return new GameRoomDetails(room, config);
    }

    @Override
    @Transactional
    public void fillWithBots(UUID roomId) {
        GameRoom room = gameRoomRepository.getForUpdate(GameRoomQuery.byId(roomId));
        if (room.getStatus() != GameRoomStatus.WAITING) {
            return;
        }
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        int botCount = config.getMaxPlayers() - room.getCurrentPlayerCount();

        if (botCount > 0) {
            botService.createBotsForRoom(roomId, botCount, config.getEntryFeeAmount());
            int newCount = room.getCurrentPlayerCount() + botCount;
            BigDecimal botPrize = config.getEntryFeeAmount().multiply(BigDecimal.valueOf(botCount));
            BigDecimal newPrize = room.getPrizePoolAmount().add(botPrize);
            gameRoomRepository.update(GameRoomQuery.byId(roomId),
                    new GameRoomPatch(null, newCount, newPrize, null, null));
        }

        int total = participantRepository.count(GameParticipantQuery.byRoom(roomId));
        if (total >= 2) {
            roundService.startRound(roomId, 1);
            notifierPort.publishRoomsUpdate(Map.of(
                    "type", "ROOM_STARTED",
                    "roomId", roomId.toString()
            ));
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
    public GameRoomDetails getRoom(UUID roomId) {
        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        return new GameRoomDetails(room, config);
    }
}
