package com.vsrna.backend.domain.balance;

import java.util.UUID;

public record PointTransactionQuery(UUID userId, UUID gameRoomId, TransactionType type, int page, int size) {
    public static PointTransactionQuery byUser(UUID userId, int page, int size) {
        return new PointTransactionQuery(userId, null, null, page, size);
    }

    public static PointTransactionQuery byRoom(UUID gameRoomId) {
        return new PointTransactionQuery(null, gameRoomId, null, 0, 100);
    }
}
