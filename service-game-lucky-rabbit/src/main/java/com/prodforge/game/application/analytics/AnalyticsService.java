package com.prodforge.game.application.analytics;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {
    GameAnalyticsSummary getSummary(Instant from, Instant to);
    List<TimeSeriesPoint> getTimeSeries(Instant from, Instant to);
    List<GameHistoryEntry> listGames(Instant from, Instant to, int page, int size);
}
