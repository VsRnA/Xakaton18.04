package com.vsrna.game.application.analytics;

public record GameAnalyticsPlayers(
        long uniqueWinners,
        double boostUsageRatePercent,
        double winnerBoostRatePercent
) {}
