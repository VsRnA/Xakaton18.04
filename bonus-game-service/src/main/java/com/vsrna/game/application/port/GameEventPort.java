package com.vsrna.game.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface GameEventPort {
    void publishGameFinished(UUID roomId, UUID winnerId, boolean winnerIsBot,
                             BigDecimal prizePool, BigDecimal prizeAwarded,
                             BigDecimal systemRevenue, String winCriteria);

    void publishEntryReserved(UUID userId, UUID roomId, BigDecimal amount);
}
