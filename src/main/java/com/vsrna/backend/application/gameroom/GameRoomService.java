package com.vsrna.backend.application.gameroom;

import com.vsrna.backend.domain.gameroom.GameRoom;
import com.vsrna.backend.domain.gameroom.GameRoomStatus;

import java.util.List;
import java.util.UUID;

public interface GameRoomService {
    GameRoom createRoom(CreateGameRoomCommand command);
    GameRoom joinRoom(UUID roomId, UUID userId);
    void fillWithBots(UUID roomId);
    List<GameRoom> listRooms(GameRoomStatus status, int page, int size);
    GameRoom getRoom(UUID roomId);
}
