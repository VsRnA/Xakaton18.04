package com.vsrna.game.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceCommandEvent(
        String commandType,
        UUID userId,
        BigDecimal amount,
        UUID roomId
) {}
