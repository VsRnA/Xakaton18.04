package com.prodforge.game.application.round;

import com.prodforge.game.domain.round.RoundResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RoundResultDetails(
        RoundResult roundResult,
        List<ParticipantScore> scores,
        UUID winnerId
) {
    public record ParticipantScore(
            UUID participantId,
            boolean isBot,
            BigDecimal totalScore,
            int selectionCount,
            Integer rank
    ) {}
}
