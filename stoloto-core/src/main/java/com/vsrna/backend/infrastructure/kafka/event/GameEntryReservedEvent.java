package com.vsrna.backend.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GameEntryReservedEvent(
        UUID userId,
        UUID roomId,
        BigDecimal amount
) {}
