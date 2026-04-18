package com.vsrna.game.infrastructure.persistence.outbox;

import com.vsrna.game.domain.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpa, UUID> {
    List<OutboxEventJpa> findByStatus(OutboxStatus status, Pageable pageable);
}
