package com.vsrna.game.application.gameroom;

import com.vsrna.game.application.gameroom.config.ConfigEvaluationResult;
import com.vsrna.game.domain.gameroom.GameRoomQuery;
import com.vsrna.game.domain.participant.GameParticipant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GameRoomService {
    GameRoomDetails createRoom(CreateGameRoomRequest request);
    GameRoomDetails joinRoom(UUID roomId, UUID userId, String displayName);
    void fillWithBots(UUID roomId);
    List<GameRoomDetails> listRooms(GameRoomQuery query);
    GameRoomDetails getRoom(UUID roomId);
    List<GameParticipant> listParticipants(UUID roomId);
    GameRoomDetails suggestRoom(BigDecimal targetEntryFee, Integer targetMaxPlayers);
    List<GameRoomDetails> affordableRooms(UUID userId, int page, int size);
    List<NextGameOption> nextGame(UUID finishedRoomId, UUID userId);
    void cancelRoom(UUID roomId, UUID adminUserId);
    void openScheduledRoom(UUID roomId);
    ConfigEvaluationResult evaluateConfig(CreateGameRoomRequest request);
}
