package com.vsrna.backend.infrastructure.persistence.balance;

import com.vsrna.backend.domain.balance.PointTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransaction, UUID> {
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<PointTransaction> findByGameRoomId(UUID gameRoomId);
}
