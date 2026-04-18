package com.vsrna.backend.domain.participant;

import java.math.BigDecimal;

public record GameParticipantPatch(ParticipantStatus status, Boolean advancedToFinal, BigDecimal reservedPoints) {
    public static GameParticipantPatch status(ParticipantStatus status) {
        return new GameParticipantPatch(status, null, null);
    }

    public static GameParticipantPatch advanceToFinal() {
        return new GameParticipantPatch(ParticipantStatus.FINALIST, true, null);
    }

    public static GameParticipantPatch eliminate() {
        return new GameParticipantPatch(ParticipantStatus.ELIMINATED, null, null);
    }
}
