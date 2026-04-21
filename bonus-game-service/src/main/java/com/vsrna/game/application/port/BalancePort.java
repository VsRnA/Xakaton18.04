package com.vsrna.game.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface BalancePort {
    BigDecimal getAvailableBalance(UUID userId);
}
