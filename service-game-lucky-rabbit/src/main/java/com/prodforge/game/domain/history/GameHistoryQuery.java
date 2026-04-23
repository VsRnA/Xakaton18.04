package com.prodforge.game.domain.history;

import java.time.Instant;
import java.util.UUID;

public record GameHistoryQuery(UUID id, UUID gameRoomId, UUID winnerUserId, Instant from, Instant to, int page, int size) {

    private static final int UNBOUNDED_SIZE = Integer.MAX_VALUE;

    public boolean isUnbounded() {
        return size == UNBOUNDED_SIZE;
    }

    public static GameHistoryQuery byId(UUID id) {
        return new GameHistoryQuery(id, null, null, null, null, 0, 1);
    }

    public static GameHistoryQuery byRoom(UUID gameRoomId) {
        return new GameHistoryQuery(null, gameRoomId, null, null, null, 0, 1);
    }

    public static GameHistoryQuery allByPeriod(Instant from, Instant to) {
        return new GameHistoryQuery(null, null, null, from, to, 0, UNBOUNDED_SIZE);
    }

    public static GameHistoryQuery byPeriod(Instant from, Instant to, int page, int size) {
        return new GameHistoryQuery(null, null, null, from, to, page, size);
    }

    public static GameHistoryQuery list(int page, int size) {
        return new GameHistoryQuery(null, null, null, null, null, page, size);
    }
}
