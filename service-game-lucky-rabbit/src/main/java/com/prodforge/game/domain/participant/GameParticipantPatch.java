package com.prodforge.game.domain.participant;

import java.math.BigDecimal;

public record GameParticipantPatch(ParticipantStatus status, Boolean advancedToFinal, BigDecimal reservedPoints, String displayName, Boolean round2Ready) {
    public static GameParticipantPatch status(ParticipantStatus status) {
        return new GameParticipantPatch(status, null, null, null, null);
    }

    public static GameParticipantPatch advanceToFinal() {
        return new GameParticipantPatch(ParticipantStatus.FINALIST, true, null, null, null);
    }

    public static GameParticipantPatch eliminate() {
        return new GameParticipantPatch(ParticipantStatus.ELIMINATED, null, null, null, null);
    }

    public static GameParticipantPatch markRound2Ready() {
        return new GameParticipantPatch(null, null, null, null, true);
    }
}
