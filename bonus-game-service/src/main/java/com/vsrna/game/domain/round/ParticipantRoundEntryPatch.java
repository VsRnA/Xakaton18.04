package com.vsrna.game.domain.round;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ParticipantRoundEntryPatch(
        Boolean boostPurchased,
        UUID boostedBarrelId,
        BigDecimal totalScore,
        Instant selectionTimestamp,
        Integer selectionCount,
        Integer rankInRound
) {
    public static ParticipantRoundEntryPatch boost() {
        return new ParticipantRoundEntryPatch(true, null, null, null, null, null);
    }

    public static ParticipantRoundEntryPatch applyBoost(UUID boostedBarrelId) {
        return new ParticipantRoundEntryPatch(null, boostedBarrelId, null, null, null, null);
    }

    public static ParticipantRoundEntryPatch selection(Instant ts, int count) {
        return new ParticipantRoundEntryPatch(null, null, null, ts, count, null);
    }

    public static ParticipantRoundEntryPatch rank(int rank, BigDecimal totalScore) {
        return new ParticipantRoundEntryPatch(null, null, totalScore, null, null, rank);
    }
}
