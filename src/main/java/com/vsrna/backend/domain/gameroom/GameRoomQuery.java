package com.vsrna.backend.domain.gameroom;

import java.util.UUID;

public record GameRoomQuery(
        UUID id,
        GameRoomStatus status,
        UUID createdByUserId,
        int page,
        int size
) {
    public static GameRoomQuery byId(UUID id) {
        return new GameRoomQuery(id, null, null, 0, 20);
    }

    public static GameRoomQuery byStatus(GameRoomStatus status) {
        return new GameRoomQuery(null, status, null, 0, 20);
    }

    public static GameRoomQuery list(int page, int size) {
        return new GameRoomQuery(null, null, null, page, size);
    }

    public static GameRoomQuery listByStatus(GameRoomStatus status, int page, int size) {
        return new GameRoomQuery(null, status, null, page, size);
    }
}
