package com.vsrna.game.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GameEntryReservedEvent(
        int version,
        UUID userId,
        UUID roomId,
        BigDecimal amount
) {
    public static final int CURRENT_VERSION = 1;
}
