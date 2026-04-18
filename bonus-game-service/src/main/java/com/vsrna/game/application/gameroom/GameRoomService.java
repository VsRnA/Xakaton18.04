package com.vsrna.game.application.gameroom;

import com.vsrna.game.domain.gameroom.GameRoomStatus;

import java.util.List;
import java.util.UUID;

public interface GameRoomService {
    GameRoomDetails createRoom(CreateGameRoomCommand command);
    GameRoomDetails joinRoom(UUID roomId, UUID userId);
    void fillWithBots(UUID roomId);
    List<GameRoomDetails> listRooms(GameRoomStatus status, int page, int size);
    GameRoomDetails getRoom(UUID roomId);
}
