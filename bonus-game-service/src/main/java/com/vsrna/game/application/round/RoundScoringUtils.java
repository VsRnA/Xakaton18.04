package com.vsrna.game.application.round;

import com.vsrna.game.domain.round.ParticipantBarrelSelection;
import com.vsrna.game.domain.round.ParticipantRoundEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RoundScoringUtils {

    private RoundScoringUtils() {}

    public static BigDecimal calculateScore(ParticipantRoundEntry entry,
                                            List<ParticipantBarrelSelection> selections,
                                            Map<UUID, BigDecimal> barrelWeights) {
        BigDecimal score = BigDecimal.ZERO;
        for (ParticipantBarrelSelection sel : selections) {
            BigDecimal w = barrelWeights.get(sel.getBarrelId());
            if (w == null) continue;
            if (sel.getBarrelId().equals(entry.getBoostedBarrelId())) {
                if (w.signum() < 0) {
                    score = score.add(w.negate());
                } else if (w.signum() > 0) {
                    score = score.add(w.multiply(RoundConstants.BOOST_MULTIPLIER));
                }
            } else {
                score = score.add(w);
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
