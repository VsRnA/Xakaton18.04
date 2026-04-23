package com.prodforge.backend.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceCommandEvent(
        int version,
        String commandType,
        UUID userId,
        BigDecimal amount,
        UUID roomId
) {}
