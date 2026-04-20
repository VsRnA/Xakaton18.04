package com.vsrna.game.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.util.UUID;

public record GameFinishedEvent(
        UUID roomId,
        UUID winnerId,
        boolean winnerIsBot,
        BigDecimal prizePool,
        BigDecimal prizeAwarded,
        BigDecimal systemRevenue,
        String winCriteria
) {}
