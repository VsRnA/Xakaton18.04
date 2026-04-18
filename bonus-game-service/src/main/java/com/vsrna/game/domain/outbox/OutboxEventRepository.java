package com.vsrna.game.domain.outbox;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {
    void save(OutboxEvent event);
    List<OutboxEvent> findPending(int limit);
    void markProcessed(UUID id);
    void markFailed(UUID id);
}
