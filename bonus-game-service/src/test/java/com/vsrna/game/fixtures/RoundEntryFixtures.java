package com.vsrna.game.fixtures;

import com.vsrna.game.domain.round.ParticipantRoundEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class RoundEntryFixtures {

    private RoundEntryFixtures() {}

    public static ParticipantRoundEntry entry(UUID roundResultId, UUID participantId) {
        ParticipantRoundEntry e = new ParticipantRoundEntry(roundResultId, participantId);
        e.setId(UUID.randomUUID());
        return e;
    }

    public static ParticipantRoundEntry rankedEntry(UUID roundResultId, UUID participantId,
                                                    int rank, BigDecimal score) {
        ParticipantRoundEntry e = entry(roundResultId, participantId);
        e.setRankInRound(rank);
        e.setTotalScore(score);
        e.setSelectionTimestamp(Instant.now());
        return e;
    }

    public static ParticipantRoundEntry boostedEntry(UUID roundResultId, UUID participantId) {
        ParticipantRoundEntry e = entry(roundResultId, participantId);
        e.setBoostPurchased(true);
        return e;
    }
}
