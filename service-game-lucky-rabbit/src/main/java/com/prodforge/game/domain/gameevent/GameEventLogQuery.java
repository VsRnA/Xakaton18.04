package com.prodforge.game.domain.gameevent;

import java.util.UUID;

public record GameEventLogQuery(UUID roomId, int page, int size) {

    public static GameEventLogQuery byRoom(UUID roomId) {
        return new GameEventLogQuery(roomId, 0, 200);
    }

    public static GameEventLogQuery byRoom(UUID roomId, int page, int size) {
        return new GameEventLogQuery(roomId, page, size);
    }
}
