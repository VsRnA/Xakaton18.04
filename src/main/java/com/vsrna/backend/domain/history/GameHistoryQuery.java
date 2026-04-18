package com.vsrna.backend.domain.history;

import java.util.UUID;

public record GameHistoryQuery(UUID id, UUID gameRoomId, UUID winnerUserId, int page, int size) {
    public static GameHistoryQuery byId(UUID id) {
        return new GameHistoryQuery(id, null, null, 0, 1);
    }

    public static GameHistoryQuery byRoom(UUID gameRoomId) {
        return new GameHistoryQuery(null, gameRoomId, null, 0, 1);
    }

    public static GameHistoryQuery list(int page, int size) {
        return new GameHistoryQuery(null, null, null, page, size);
    }
}
