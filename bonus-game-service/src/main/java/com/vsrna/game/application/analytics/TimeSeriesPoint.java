package com.vsrna.game.application.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeSeriesPoint(
        LocalDate date,
        long gamesCount,
        BigDecimal realRevenue,
        BigDecimal prizesAwarded,
        BigDecimal retained,
        long botWins,
        long realWins
) {}
