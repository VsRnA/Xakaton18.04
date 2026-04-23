package com.vsrna.backend.infrastructure.persistence.balance;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransactionJpa, UUID> {
    List<PointTransactionJpa> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<PointTransactionJpa> findByGameRoomId(UUID gameRoomId);
    Optional<PointTransactionJpa> findByIdempotencyKey(String idempotencyKey);
}
