package com.vsrna.game.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceCommandEvent(
        int version,
        String commandType,
        UUID userId,
        BigDecimal amount,
        UUID roomId
) {
    public static final int CURRENT_VERSION = 1;
}
