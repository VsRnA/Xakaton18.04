package com.vsrna.game.infrastructure.persistence.outbox;

import com.vsrna.game.domain.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpa, UUID> {
    List<OutboxEventJpa> findByStatus(OutboxStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEventJpa e SET e.status = :status, e.processedAt = :processedAt WHERE e.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") OutboxStatus status,
                      @Param("processedAt") Instant processedAt);
}
