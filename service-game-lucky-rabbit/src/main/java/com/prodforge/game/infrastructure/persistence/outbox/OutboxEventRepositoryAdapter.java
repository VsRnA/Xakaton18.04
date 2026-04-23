package com.prodforge.game.infrastructure.persistence.outbox;

import com.prodforge.game.domain.outbox.OutboxEvent;
import com.prodforge.game.domain.outbox.OutboxEventRepository;
import com.prodforge.game.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
        return jpa.findByStatus(OutboxStatus.PENDING, PageRequest.of(0, limit, Sort.by("createdAt").ascending()))
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void markProcessed(UUID id) {
        jpa.updateStatus(id, OutboxStatus.PROCESSED, Instant.now());
    }

    @Override
    @Transactional
    public void markFailed(UUID id) {
        jpa.updateStatus(id, OutboxStatus.FAILED, null);
    }

    private OutboxEvent toDomain(OutboxEventJpa jpaEntity) {
        OutboxEvent event = new OutboxEvent();
        event.setId(jpaEntity.getId());
        event.setAggregateType(jpaEntity.getAggregateType());
        event.setAggregateId(jpaEntity.getAggregateId());
        event.setEventType(jpaEntity.getEventType());
        event.setTopic(jpaEntity.getTopic());
        event.setPayload(jpaEntity.getPayload());
        event.setStatus(jpaEntity.getStatus());
        event.setCreatedAt(jpaEntity.getCreatedAt());
        event.setProcessedAt(jpaEntity.getProcessedAt());
        return event;
    }

    private OutboxEventJpa toJpa(OutboxEvent event) {
        OutboxEventJpa jpaEntity = new OutboxEventJpa();
        jpaEntity.setId(event.getId());
        jpaEntity.setAggregateType(event.getAggregateType());
        jpaEntity.setAggregateId(event.getAggregateId());
        jpaEntity.setEventType(event.getEventType());
        jpaEntity.setTopic(event.getTopic());
        jpaEntity.setPayload(event.getPayload());
        jpaEntity.setStatus(event.getStatus());
        jpaEntity.setProcessedAt(event.getProcessedAt());
        return jpaEntity;
    }
}
