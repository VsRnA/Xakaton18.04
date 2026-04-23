package com.prodforge.game.domain.history;

import java.math.BigDecimal;

public interface GameHistoryAnalyticsRepository {
    BigDecimal getCumulativeSystemBalance();
}
