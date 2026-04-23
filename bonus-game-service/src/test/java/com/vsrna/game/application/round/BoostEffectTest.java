package com.vsrna.game.application.round;

import com.vsrna.game.application.round.scoring.RoundScoringUtils;
import com.vsrna.game.fixtures.BarrelSelectionFixtures;
import com.vsrna.game.fixtures.RoundEntryFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BoostEffectTest {

    @Test
    void negativeBarrels_negatesTheMostNegative() {
        UUID weakBarrel  = UUID.randomUUID();
        UUID strongBarrel = UUID.randomUUID();
        var entry = RoundEntryFixtures.entry(UUID.randomUUID(), UUID.randomUUID());
        var selections = List.of(
                BarrelSelectionFixtures.selection(entry.getId(), weakBarrel),   // weight = -3
                BarrelSelectionFixtures.selection(entry.getId(), strongBarrel)  // weight = -8 ← most negative
        );
        var weights = Map.of(weakBarrel, new BigDecimal("-3"), strongBarrel, new BigDecimal("-8"));

        var effect = RoundScoringUtils.computeBoostEffect(selections, weights);

        assertThat(effect.barrelId()).isEqualTo(strongBarrel);
        assertThat(effect.originalWeight()).isEqualByComparingTo(new BigDecimal("-8"));
        assertThat(effect.boostedWeight()).isEqualByComparingTo(new BigDecimal("8"));
    }

    @Test
    void allPositive_doublesTheMinimum() {
        UUID smallBarrel = UUID.randomUUID();
        UUID largeBarrel = UUID.randomUUID();
        var entry = RoundEntryFixtures.entry(UUID.randomUUID(), UUID.randomUUID());
        var selections = List.of(
                BarrelSelectionFixtures.selection(entry.getId(), smallBarrel),  // weight = 2 ← minimum
                BarrelSelectionFixtures.selection(entry.getId(), largeBarrel)   // weight = 7
        );
        var weights = Map.of(smallBarrel, new BigDecimal("2"), largeBarrel, new BigDecimal("7"));

        var effect = RoundScoringUtils.computeBoostEffect(selections, weights);

        assertThat(effect.barrelId()).isEqualTo(smallBarrel);
        assertThat(effect.originalWeight()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(effect.boostedWeight()).isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void emptySelections_returnsNull() {
        var effect = RoundScoringUtils.computeBoostEffect(List.of(), Map.of());

        assertThat(effect).isNull();
    }

    @Test
    void mixedWeights_prefersNegativeOverPositive() {
        UUID negativeBarrel = UUID.randomUUID();
        UUID positiveBarrel = UUID.randomUUID();
        var entry = RoundEntryFixtures.entry(UUID.randomUUID(), UUID.randomUUID());
        var selections = List.of(
                BarrelSelectionFixtures.selection(entry.getId(), negativeBarrel),
                BarrelSelectionFixtures.selection(entry.getId(), positiveBarrel)
        );
        var weights = Map.of(negativeBarrel, new BigDecimal("-5"), positiveBarrel, new BigDecimal("10"));

        var effect = RoundScoringUtils.computeBoostEffect(selections, weights);

        assertThat(effect.barrelId()).isEqualTo(negativeBarrel);
    }
}
