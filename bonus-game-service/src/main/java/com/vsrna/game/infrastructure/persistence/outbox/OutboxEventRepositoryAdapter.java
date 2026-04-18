package com.vsrna.game.infrastructure.persistence.outbox;

import com.vsrna.game.domain.outbox.OutboxEvent;
import com.vsrna.game.domain.outbox.OutboxEventRepository;
import com.vsrna.game.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpa;

    @Override
    public void save(OutboxEvent event) {
        jpa.save(toJpa(event));
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return jpa.findByStatus(OutboxStatus.PENDING, PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void markProcessed(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setStatus(OutboxStatus.PROCESSED);
            e.setProcessedAt(Instant.now());
            jpa.save(e);
        });
    }

    @Override
    public void markFailed(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setStatus(OutboxStatus.FAILED);
            jpa.save(e);
        });
    }

    private OutboxEvent toDomain(OutboxEventJpa e) {
        OutboxEvent event = new OutboxEvent();
        event.setId(e.getId());
        event.setAggregateType(e.getAggregateType());
        event.setAggregateId(e.getAggregateId());
        event.setEventType(e.getEventType());
        event.setTopic(e.getTopic());
        event.setPayload(e.getPayload());
        event.setStatus(e.getStatus());
        event.setCreatedAt(e.getCreatedAt());
        event.setProcessedAt(e.getProcessedAt());
        return event;
    }

    private OutboxEventJpa toJpa(OutboxEvent event) {
        OutboxEventJpa e = new OutboxEventJpa();
        e.setId(event.getId());
        e.setAggregateType(event.getAggregateType());
        e.setAggregateId(event.getAggregateId());
        e.setEventType(event.getEventType());
        e.setTopic(event.getTopic());
        e.setPayload(event.getPayload());
        e.setStatus(event.getStatus());
        e.setProcessedAt(event.getProcessedAt());
        return e;
    }
}
