package com.vsrna.game.domain.round;

import java.math.BigDecimal;
import java.time.Instant;

public record ParticipantRoundEntryPatch(
        Boolean boostPurchased,
        BigDecimal totalScore,
        Instant selectionTimestamp,
        Integer selectionCount,
        Integer rankInRound
) {
    public static ParticipantRoundEntryPatch boost() {
        return new ParticipantRoundEntryPatch(true, null, null, null, null);
    }

    public static ParticipantRoundEntryPatch selection(Instant ts, int count) {
        return new ParticipantRoundEntryPatch(null, null, ts, count, null);
    }

    public static ParticipantRoundEntryPatch rank(int rank, BigDecimal totalScore) {
        return new ParticipantRoundEntryPatch(null, totalScore, null, null, rank);
    }
}
