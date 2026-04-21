package com.vsrna.game.fixtures;

import com.vsrna.game.domain.participant.GameParticipant;
import com.vsrna.game.domain.participant.ParticipantStatus;

import java.math.BigDecimal;
import java.util.UUID;

public final class GameParticipantFixtures {

    private GameParticipantFixtures() {}

    public static GameParticipant realParticipant(UUID roomId, UUID userId) {
        GameParticipant p = new GameParticipant(roomId, userId, false, "Player", new BigDecimal("100"));
        p.setId(UUID.randomUUID());
        p.setStatus(ParticipantStatus.ACTIVE);
        return p;
    }

    public static GameParticipant finalist(UUID roomId, UUID userId) {
        GameParticipant p = realParticipant(roomId, userId);
        p.setStatus(ParticipantStatus.FINALIST);
        return p;
    }

    public static GameParticipant botParticipant(UUID roomId) {
        GameParticipant p = new GameParticipant(roomId, null, true, "Bot", new BigDecimal("100"));
        p.setId(UUID.randomUUID());
        p.setStatus(ParticipantStatus.FINALIST);
        return p;
    }
}
