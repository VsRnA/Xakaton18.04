package com.vsrna.game.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Порт для публикации доменных событий в шину (Kafka / outbox).
 * Application layer не зависит от Kafka/outbox напрямую.
 */
public interface GameEventPort {
    void publishGameFinished(UUID roomId, UUID winnerId, boolean winnerIsBot,
                             BigDecimal prizePool, BigDecimal prizeAwarded,
                             BigDecimal systemRevenue, String winCriteria);

    void publishEntryReserved(UUID userId, UUID roomId, BigDecimal amount);
}
