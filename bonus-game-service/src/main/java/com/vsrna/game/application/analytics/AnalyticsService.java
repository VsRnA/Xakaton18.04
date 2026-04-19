package com.vsrna.game.application.analytics;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {
    GameAnalyticsSummary getSummary(Instant from, Instant to);

    /** Данные для графиков: по одной точке на день в указанном периоде. */
    List<TimeSeriesPoint> getTimeSeries(Instant from, Instant to);
}
