package com.vsrna.backend.domain.balance;

import java.util.List;
import java.util.Optional;

public interface PointTransactionRepository {
    PointTransaction create(PointTransaction transaction);
    List<PointTransaction> list(PointTransactionQuery query);
    Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey);
}
