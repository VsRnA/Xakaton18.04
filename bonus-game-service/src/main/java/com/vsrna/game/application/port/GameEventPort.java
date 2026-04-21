package com.vsrna.game.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface GameEventPort {
    void publishGameFinished(UUID roomId, UUID winnerId, boolean winnerIsBot,
                             BigDecimal prizePool, BigDecimal prizeAwarded,
                             BigDecimal systemRevenue, String winCriteria);

    void publishEntryReserved(UUID userId, UUID roomId, BigDecimal amount);

    // Balance commands — written to outbox atomically with business state changes
    void publishBalanceReserve(UUID userId, BigDecimal amount, UUID roomId);
    void publishBalanceRelease(UUID userId, BigDecimal amount, UUID roomId);
    void publishBalanceAward(UUID userId, BigDecimal amount, UUID roomId);
    void publishBalanceDeduct(UUID userId, BigDecimal amount, UUID roomId);
}
