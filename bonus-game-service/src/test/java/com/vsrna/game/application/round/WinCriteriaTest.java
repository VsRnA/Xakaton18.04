package com.vsrna.game.application.round;

import com.vsrna.game.application.round.scoring.RoundScoringUtils;
import com.vsrna.game.fixtures.RoundEntryFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WinCriteriaTest {

    @Test
    void differentScores_returnsScore() {
        var roundResultId = UUID.randomUUID();
        var winner = RoundEntryFixtures.rankedEntry(roundResultId, UUID.randomUUID(), 1, new BigDecimal("10"));
        var loser  = RoundEntryFixtures.rankedEntry(roundResultId, UUID.randomUUID(), 2, new BigDecimal("5"));

        assertThat(RoundScoringUtils.determineWinCriteria(List.of(winner, loser))).isEqualTo("SCORE");
    }

    @Test
    void sameScore_differentSelectionCount_returnsSelectionCountTiebreak() {
        var roundResultId = UUID.randomUUID();
        var winner = RoundEntryFixtures.rankedEntry(roundResultId, UUID.randomUUID(), 1, new BigDecimal("10"));
        var loser  = RoundEntryFixtures.rankedEntry(roundResultId, UUID.randomUUID(), 2, new BigDecimal("10"));
        winner.setSelectionCount(3);
        loser.setSelectionCount(1);

        assertThat(RoundScoringUtils.determineWinCriteria(List.of(winner, loser))).isEqualTo("SELECTION_COUNT_TIEBREAK");
    }

    @Test
    void sameScoreAndCount_returnsTimestampTiebreak() {
        var roundResultId = UUID.randomUUID();
        var winner = RoundEntryFixtures.rankedEntry(roundResultId, UUID.randomUUID(), 1, new BigDecimal("10"));
        var loser  = RoundEntryFixtures.rankedEntry(roundResultId, UUID.randomUUID(), 2, new BigDecimal("10"));
        winner.setSelectionCount(2);
        loser.setSelectionCount(2);
        winner.setSelectionTimestamp(Instant.parse("2024-01-01T10:00:00Z"));
        loser.setSelectionTimestamp(Instant.parse("2024-01-01T10:00:05Z"));

        assertThat(RoundScoringUtils.determineWinCriteria(List.of(winner, loser))).isEqualTo("TIMESTAMP_TIEBREAK");
    }

    @Test
    void singleParticipant_returnsScore() {
        var entry = RoundEntryFixtures.rankedEntry(UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("10"));

        assertThat(RoundScoringUtils.determineWinCriteria(List.of(entry))).isEqualTo("SCORE");
    }
}
