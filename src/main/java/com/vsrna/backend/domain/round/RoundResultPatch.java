package com.vsrna.backend.domain.round;

import java.time.Instant;

public record RoundResultPatch(RoundResultStatus status, String seedHash, String rawSeed, Instant endedAt) {
    public static RoundResultPatch boostWindow(String seedHash, String rawSeed) {
        return new RoundResultPatch(RoundResultStatus.BOOST_WINDOW, seedHash, rawSeed, null);
    }

    public static RoundResultPatch completed(Instant endedAt) {
        return new RoundResultPatch(RoundResultStatus.COMPLETED, null, null, endedAt);
    }
}
