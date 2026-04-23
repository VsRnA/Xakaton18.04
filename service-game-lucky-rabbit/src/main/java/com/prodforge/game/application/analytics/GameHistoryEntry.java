package com.prodforge.game.application.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GameHistoryEntry(
        UUID gameRoomId,
        Instant completedAt,
        UUID winnerUserId,
        boolean winnerIsBot,
        BigDecimal entryFeeAmount,
        BigDecimal realPlayersRevenue,
        BigDecimal prizeAwarded,
        BigDecimal systemBalance,
        String winCriteria,
        int realPlayersCount,
        int botCount,
        boolean boostAvailable,
        int boostUsedCount,
        BigDecimal boostRevenue
) {}
