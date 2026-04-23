package com.prodforge.game.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GameFinishedEvent(
        int version,
        UUID roomId,
        UUID winnerId,
        boolean winnerIsBot,
        BigDecimal prizePool,
        BigDecimal prizeAwarded,
        BigDecimal systemRevenue,
        String winCriteria
) {
    public static final int CURRENT_VERSION = 1;
}
