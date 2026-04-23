package com.vsrna.game.application.round.history;

import java.math.BigDecimal;
import java.util.UUID;

public record ParticipantHistoryEntry(
        UUID participantId,
        UUID userId,
        boolean isBot,
        String displayName,
        boolean boostPurchased,
        BigDecimal totalScore,
        Integer rank,
        boolean isWinner
) {}
