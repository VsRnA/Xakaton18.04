package com.vsrna.game.application.gameroom;

import com.vsrna.game.domain.gameroom.GameRoomQuery;
import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.domain.participant.GameParticipant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
// NextGameOption, ConfigEvaluationResult are in the same package

public interface GameRoomService {
    GameRoomDetails createRoom(CreateGameRoomCommand command);
    GameRoomDetails joinRoom(UUID roomId, UUID userId, String displayName);
    void fillWithBots(UUID roomId);
    List<GameRoomDetails> listRooms(GameRoomStatus status, int page, int size);
    List<GameRoomDetails> listRoomsFiltered(GameRoomQuery query);
    GameRoomDetails getRoom(UUID roomId);
    List<GameParticipant> listParticipants(UUID roomId);
    GameRoomDetails suggestRoom(BigDecimal targetEntryFee, Integer targetMaxPlayers);
    List<NextGameOption> nextGame(UUID finishedRoomId);
    void cancelRoom(UUID roomId, UUID adminUserId);
    ConfigEvaluationResult evaluateConfig(CreateGameRoomCommand command);
}
