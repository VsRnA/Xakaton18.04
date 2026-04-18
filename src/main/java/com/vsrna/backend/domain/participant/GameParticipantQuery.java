package com.vsrna.backend.domain.participant;

import java.util.UUID;

public record GameParticipantQuery(
        UUID id,
        UUID gameRoomId,
        UUID userId,
        ParticipantStatus status,
        Boolean isBot,
        int page,
        int size
) {
    public static GameParticipantQuery byId(UUID id) {
        return new GameParticipantQuery(id, null, null, null, null, 0, 100);
    }

    public static GameParticipantQuery byRoom(UUID gameRoomId) {
        return new GameParticipantQuery(null, gameRoomId, null, null, null, 0, 100);
    }

    public static GameParticipantQuery byRoomAndUser(UUID gameRoomId, UUID userId) {
        return new GameParticipantQuery(null, gameRoomId, userId, null, null, 0, 1);
    }

    public static GameParticipantQuery byRoomAndStatus(UUID gameRoomId, ParticipantStatus status) {
        return new GameParticipantQuery(null, gameRoomId, null, status, null, 0, 100);
    }
}
