package com.prodforge.game.application.analytics;

import java.time.Instant;

public record GameAnalyticsSummary(
        Instant from,
        Instant to,
        GameAnalyticsEconomics economics,
        GameAnalyticsRooms rooms,
        GameAnalyticsPlayers players
) {}
