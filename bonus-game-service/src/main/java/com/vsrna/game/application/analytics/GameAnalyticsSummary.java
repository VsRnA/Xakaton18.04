package com.vsrna.game.application.analytics;

import java.math.BigDecimal;
import java.time.Instant;

public record GameAnalyticsSummary(

        Instant from,
        Instant to,

        long totalGames,
        BigDecimal totalRealRevenue,
        BigDecimal totalPrizesAwarded,
        BigDecimal totalBoostRevenue,
        BigDecimal totalRetained,
        double retentionRatePercent,
        BigDecimal cumulativeSystemBalance,

        long botWins,
        long realPlayerWins,
        double botWinRatePercent,
        double avgRealPlayersPerRoom,
        double avgBotFillRate,

        long uniqueWinners,
        double boostUsageRatePercent,

        double winnerBoostRatePercent

) {}
