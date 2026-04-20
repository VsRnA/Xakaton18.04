package com.vsrna.game.application.round;

import com.vsrna.game.domain.round.ParticipantBarrelSelection;
import com.vsrna.game.domain.round.ParticipantRoundEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoundScoringUtils {

    private static final Logger log = LoggerFactory.getLogger(RoundScoringUtils.class);

    private RoundScoringUtils() {}

    public record BoostEffect(UUID barrelId, BigDecimal originalWeight, BigDecimal boostedWeight) {}

    /**
     * Вычисляет эффект буста для набора выбранных бочек:
     * 1. Если есть отрицательные веса — меняет знак наибольшей по модулю отрицательной бочки
     * 2. Если все веса положительные — удваивает минимальную положительную бочку
     */
    public static BoostEffect computeBoostEffect(List<ParticipantBarrelSelection> selections,
                                                  Map<UUID, BigDecimal> barrelWeights) {
        if (selections.isEmpty()) return null;

        ParticipantBarrelSelection mostNegativeTarget = null;
        BigDecimal mostNegativeWeight = null;

        ParticipantBarrelSelection minPositiveTarget = null;
        BigDecimal minPositiveWeight = null;

        for (ParticipantBarrelSelection sel : selections) {
            BigDecimal w = barrelWeights.get(sel.getBarrelId());
            if (w == null) continue;
            if (w.signum() < 0) {
                if (mostNegativeWeight == null || w.compareTo(mostNegativeWeight) < 0) {
                    mostNegativeWeight = w;
                    mostNegativeTarget = sel;
                }
            } else if (w.signum() > 0) {
                if (minPositiveWeight == null || w.compareTo(minPositiveWeight) < 0) {
                    minPositiveWeight = w;
                    minPositiveTarget = sel;
                }
            }
        }

        if (mostNegativeTarget != null) {
            return new BoostEffect(mostNegativeTarget.getBarrelId(), mostNegativeWeight, mostNegativeWeight.negate());
        }
        if (minPositiveTarget != null) {
            return new BoostEffect(minPositiveTarget.getBarrelId(), minPositiveWeight, minPositiveWeight.multiply(BigDecimal.TWO));
        }
        return null;
    }

    public static BigDecimal calculateScore(ParticipantRoundEntry entry,
                                            List<ParticipantBarrelSelection> selections,
                                            Map<UUID, BigDecimal> barrelWeights) {
        BoostEffect boostEffect = entry.isBoostPurchased()
                ? computeBoostEffect(selections, barrelWeights) : null;

        BigDecimal score = BigDecimal.ZERO;
        for (ParticipantBarrelSelection sel : selections) {
            BigDecimal barrelWeight = barrelWeights.get(sel.getBarrelId());
            if (barrelWeight == null) {
                log.warn("No weight found for barrelId={}, skipping in score calculation", sel.getBarrelId());
                continue;
            }
            if (boostEffect != null && sel.getBarrelId().equals(boostEffect.barrelId())) {
                score = score.add(boostEffect.boostedWeight());
            } else {
                score = score.add(barrelWeight);
            }
        }
        return score;
    }

    public static String determineWinCriteria(List<ParticipantRoundEntry> sorted) {
        if (sorted.size() < 2) return "SCORE";
        ParticipantRoundEntry first = sorted.get(0);
        ParticipantRoundEntry second = sorted.get(1);
        if (first.getTotalScore() != null && second.getTotalScore() != null
                && first.getTotalScore().compareTo(second.getTotalScore()) != 0) {
            return "SCORE";
        }
        if (first.getSelectionCount() != second.getSelectionCount()) {
            return "SELECTION_COUNT_TIEBREAK";
        }
        return "TIMESTAMP_TIEBREAK";
    }
}
