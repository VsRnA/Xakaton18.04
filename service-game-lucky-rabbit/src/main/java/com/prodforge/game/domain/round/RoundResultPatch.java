package com.prodforge.game.domain.round;

import java.time.Instant;

public record RoundResultPatch(RoundResultStatus status, String seedHash, String rawSeed, Instant endedAt) {

    public static RoundResultPatch commit(String seedHash, String rawSeed) {
        return new RoundResultPatch(null, seedHash, rawSeed, null);
    }

    public static RoundResultPatch boostWindow() {
        return new RoundResultPatch(RoundResultStatus.BOOST_WINDOW, null, null, null);
    }

    public static RoundResultPatch completed(Instant endedAt) {
        return new RoundResultPatch(RoundResultStatus.COMPLETED, null, null, endedAt);
    }
}
