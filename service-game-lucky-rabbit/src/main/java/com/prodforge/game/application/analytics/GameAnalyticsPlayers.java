package com.prodforge.game.application.analytics;

public record GameAnalyticsPlayers(
        long uniqueWinners,
        double boostUsageRatePercent,
        double winnerBoostRatePercent
) {}
