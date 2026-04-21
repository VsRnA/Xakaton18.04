package com.vsrna.game.application.round;

import com.vsrna.game.fixtures.BarrelSelectionFixtures;
import com.vsrna.game.fixtures.RoundEntryFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreCalculationTest {

    @Test
    void noBoost_returnsSumOfWeights() {
        UUID barrel1 = UUID.randomUUID();
        UUID barrel2 = UUID.randomUUID();
        var entry = RoundEntryFixtures.entry(UUID.randomUUID(), UUID.randomUUID());
        var selections = List.of(
                BarrelSelectionFixtures.selection(entry.getId(), barrel1),
                BarrelSelectionFixtures.selection(entry.getId(), barrel2)
        );
        var weights = Map.of(barrel1, new BigDecimal("5"), barrel2, new BigDecimal("3"));

        BigDecimal score = RoundScoringUtils.calculateScore(entry, selections, weights);

        assertThat(score).isEqualByComparingTo(new BigDecimal("8"));
    }

    @Test
    void withBoost_negativeBarrelGetsBoosted() {
        UUID badBarrel  = UUID.randomUUID();
        UUID goodBarrel = UUID.randomUUID();
        var entry = RoundEntryFixtures.boostedEntry(UUID.randomUUID(), UUID.randomUUID());
        var selections = List.of(
                BarrelSelectionFixtures.selection(entry.getId(), badBarrel),   // -4 → boost → +4
                BarrelSelectionFixtures.selection(entry.getId(), goodBarrel)   // 6
        );
        var weights = Map.of(badBarrel, new BigDecimal("-4"), goodBarrel, new BigDecimal("6"));

        BigDecimal score = RoundScoringUtils.calculateScore(entry, selections, weights);

        assertThat(score).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void emptySelections_returnsZero() {
        var entry = RoundEntryFixtures.entry(UUID.randomUUID(), UUID.randomUUID());

        BigDecimal score = RoundScoringUtils.calculateScore(entry, List.of(), Map.of());

        assertThat(score).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
