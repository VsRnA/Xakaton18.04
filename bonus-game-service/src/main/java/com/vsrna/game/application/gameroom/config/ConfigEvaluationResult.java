package com.vsrna.game.application.gameroom.config;

import java.math.BigDecimal;
import java.util.List;

public record ConfigEvaluationResult(
        BigDecimal projectedPrizePool,
        BigDecimal projectedSystemRevenue,
        double systemRevenuePercent,
        double playerExpectedValue,
        String attractivenessScore,
        List<ConfigWarning> warnings
) {}
