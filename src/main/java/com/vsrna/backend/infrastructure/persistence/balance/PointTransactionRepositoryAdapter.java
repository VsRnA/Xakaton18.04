package com.vsrna.backend.infrastructure.persistence.balance;

import com.vsrna.backend.domain.balance.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointTransactionRepositoryAdapter implements PointTransactionRepository {

    private final PointTransactionJpaRepository jpa;

    @Override
    public PointTransaction create(PointTransaction transaction) {
        return jpa.save(transaction);
    }

    @Override
    public List<PointTransaction> list(PointTransactionQuery query) {
        if (query.userId() != null) {
            int page = Math.max(query.page(), 0);
            int size = query.size() > 0 ? query.size() : 20;
            return jpa.findByUserIdOrderByCreatedAtDesc(query.userId(), PageRequest.of(page, size));
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId());
        }
        return List.of();
    }
}
