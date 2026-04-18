package com.vsrna.backend.domain.round;

import java.util.UUID;

public record RoundResultQuery(UUID id, UUID gameRoomId, Integer roundNumber, RoundResultStatus status) {
    public static RoundResultQuery byId(UUID id) {
        return new RoundResultQuery(id, null, null, null);
    }

    public static RoundResultQuery byRoomAndRound(UUID gameRoomId, int roundNumber) {
        return new RoundResultQuery(null, gameRoomId, roundNumber, null);
    }
}
