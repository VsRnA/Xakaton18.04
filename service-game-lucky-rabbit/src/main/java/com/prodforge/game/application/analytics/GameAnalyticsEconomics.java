package com.prodforge.game.application.analytics;

import java.math.BigDecimal;

public record GameAnalyticsEconomics(
        BigDecimal totalRealRevenue,
        BigDecimal totalPrizesAwarded,
        BigDecimal totalBoostRevenue,
        BigDecimal totalRetained,
        double retentionRatePercent,
        BigDecimal cumulativeSystemBalance
) {}
