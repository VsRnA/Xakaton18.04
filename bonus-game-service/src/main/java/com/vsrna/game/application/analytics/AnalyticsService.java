package com.vsrna.game.application.analytics;

import com.vsrna.game.domain.history.GameHistory;

import java.time.Instant;
import java.util.List;

public interface AnalyticsService {
    /** from/to могут быть null — сервис использует последние 30 дней по умолчанию */
    GameAnalyticsSummary getSummary(Instant from, Instant to);
    List<TimeSeriesPoint> getTimeSeries(Instant from, Instant to);
    List<GameHistory> listGames(Instant from, Instant to, int page, int size);
}
