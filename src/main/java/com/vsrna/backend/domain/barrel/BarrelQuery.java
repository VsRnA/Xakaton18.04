package com.vsrna.backend.domain.barrel;

import java.util.UUID;

public record BarrelQuery(UUID id, UUID gameRoomId, Integer roundNumber) {
    public static BarrelQuery byId(UUID id) {
        return new BarrelQuery(id, null, null);
    }

    public static BarrelQuery byRoom(UUID gameRoomId) {
        return new BarrelQuery(null, gameRoomId, null);
    }

    public static BarrelQuery byRoomAndRound(UUID gameRoomId, int roundNumber) {
        return new BarrelQuery(null, gameRoomId, roundNumber);
    }
}
