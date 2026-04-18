package com.vsrna.backend.application.gameroom;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.barrel.*;
import com.vsrna.backend.domain.gameroom.*;
import com.vsrna.backend.domain.participant.*;
import com.vsrna.backend.application.balance.UserBalanceService;
import com.vsrna.backend.application.bot.BotService;
import com.vsrna.backend.application.round.RoundService;
import com.vsrna.backend.infrastructure.scheduler.GameScheduler;
import com.vsrna.backend.infrastructure.websocket.GameWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final UserBalanceService userBalanceService;
    private final BotService botService;
    private final GameScheduler scheduler;
    private final GameWebSocketPublisher wsPublisher;
    @Lazy
    private final RoundService roundService;

    @Override
    @Transactional
    public GameRoom createRoom(CreateGameRoomCommand command) {
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

        return room;
    }

    @Override
    @Transactional
    public GameRoom joinRoom(UUID roomId, UUID userId) {
        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        if (room.getStatus() != GameRoomStatus.WAITING) {
            throw ApiException.badRequest("Room is not accepting players");
        }
        if (room.getCurrentPlayerCount() >= config.getMaxPlayers()) {
            throw ApiException.badRequest("Room is full");
        }
        if (participantRepository.find(GameParticipantQuery.byRoomAndUser(roomId, userId)).isPresent()) {
            throw ApiException.alreadyExists("GameParticipant", "User already joined this room");
        }

        userBalanceService.reservePoints(userId, config.getEntryFeeAmount(), roomId);

        GameParticipant participant = new GameParticipant(roomId, userId, false, config.getEntryFeeAmount());
        participantRepository.create(participant);

        int newCount = room.getCurrentPlayerCount() + 1;
        BigDecimal newPrize = room.getPrizePoolAmount().add(config.getEntryFeeAmount());
        room = gameRoomRepository.update(
                GameRoomQuery.byId(roomId),
                new GameRoomPatch(null, newCount, newPrize, null, null)
        );

        if (newCount == 1) {
            final UUID finalRoomId = roomId;
            scheduler.scheduleWaitTimerExpiry(roomId, () -> fillWithBots(finalRoomId));
        }

        int playerCount = room.getCurrentPlayerCount();
        wsPublisher.publishRoomUpdate(roomId, new java.util.HashMap<>() {{
            put("type", "ROOM_UPDATED");
            put("currentPlayers", playerCount);
            put("prizePool", newPrize);
            put("winProbability", playerCount > 0 ? 1.0 / playerCount : 1.0);
        }});

        return room;
    }

    @Override
    @Transactional
    public void fillWithBots(UUID roomId) {
        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
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
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameRoom> listRooms(GameRoomStatus status, int page, int size) {
        return gameRoomRepository.list(new GameRoomQuery(null, status, null, page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public GameRoom getRoom(UUID roomId) {
        return gameRoomRepository.get(GameRoomQuery.byId(roomId));
    }
}
