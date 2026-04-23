package com.prodforge.backend.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GameEntryReservedEvent(
        int version,
        UUID userId,
        UUID roomId,
        BigDecimal amount
) {}
