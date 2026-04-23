package com.prodforge.backend.infrastructure.persistence.balance;

import com.prodforge.backend.domain.balance.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PointTransactionRepositoryAdapter implements PointTransactionRepository {

    private final PointTransactionJpaRepository jpa;

    @Override
    public PointTransaction create(PointTransaction transaction) {
        return toDomain(jpa.save(toJpa(transaction)));
    }

    @Override
    public List<PointTransaction> list(PointTransactionQuery query) {
        if (query.userId() != null) {
            int page = Math.max(query.page(), 0);
            int size = query.size() > 0 ? query.size() : 20;
            return jpa.findByUserIdOrderByCreatedAtDesc(query.userId(), PageRequest.of(page, size))
                    .stream().map(this::toDomain).collect(Collectors.toList());
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId())
                    .stream().map(this::toDomain).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private PointTransaction toDomain(PointTransactionJpa j) {
        PointTransaction t = new PointTransaction();
        t.setId(j.getId());
        t.setUserId(j.getUserId());
        t.setType(j.getType());
        t.setAmount(j.getAmount());
        t.setGameRoomId(j.getGameRoomId());
        t.setDescription(j.getDescription());
        t.setIdempotencyKey(j.getIdempotencyKey());
        t.setCreatedAt(j.getCreatedAt());
        return t;
    }

    private PointTransactionJpa toJpa(PointTransaction t) {
        PointTransactionJpa j = new PointTransactionJpa();
        j.setId(t.getId());
        j.setUserId(t.getUserId());
        j.setType(t.getType());
        j.setAmount(t.getAmount());
        j.setGameRoomId(t.getGameRoomId());
        j.setDescription(t.getDescription());
        j.setIdempotencyKey(t.getIdempotencyKey());
        return j;
    }
}
