package com.vsrna.game.application.round;

import com.vsrna.game.domain.round.ParticipantRoundEntry;

import java.util.List;

public final class RoundScoringUtils {

    private RoundScoringUtils() {}

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
