package com.prodforge.game.application.analytics;

public record GameAnalyticsRooms(
        long totalGames,
        long botWins,
        long realPlayerWins,
        double botWinRatePercent,
        double avgRealPlayersPerRoom,
        double avgBotFillRate
) {}
