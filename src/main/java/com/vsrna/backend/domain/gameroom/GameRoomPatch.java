package com.vsrna.backend.domain.gameroom;

import java.math.BigDecimal;
import java.time.Instant;

public record GameRoomPatch(
        GameRoomStatus status,
        Integer currentPlayerCount,
        BigDecimal prizePoolAmount,
        Instant startedAt,
        Instant finishedAt
) {
    public static GameRoomPatch status(GameRoomStatus status) {
        return new GameRoomPatch(status, null, null, null, null);
    }

    public static GameRoomPatch playerCount(int count) {
        return new GameRoomPatch(null, count, null, null, null);
    }

    public static GameRoomPatch prizePool(BigDecimal prizePoolAmount) {
        return new GameRoomPatch(null, null, prizePoolAmount, null, null);
    }

    public static GameRoomPatch started(GameRoomStatus status, int playerCount, Instant startedAt) {
        return new GameRoomPatch(status, playerCount, null, startedAt, null);
    }

    public static GameRoomPatch finished(Instant finishedAt) {
        return new GameRoomPatch(GameRoomStatus.FINISHED, null, null, null, finishedAt);
    }
}
