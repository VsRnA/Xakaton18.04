package com.vsrna.backend.domain.gameroom;

public interface GameRoomConfigRepository {
    GameRoomConfig create(GameRoomConfig config);
    GameRoomConfig get(GameRoomConfigQuery query);
}
