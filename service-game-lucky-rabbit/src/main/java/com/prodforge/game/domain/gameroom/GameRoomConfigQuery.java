package com.prodforge.game.domain.gameroom;

import java.util.UUID;

public record GameRoomConfigQuery(UUID gameRoomId) {
    public static GameRoomConfigQuery byRoom(UUID gameRoomId) {
        return new GameRoomConfigQuery(gameRoomId);
    }
}
