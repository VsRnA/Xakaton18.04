package com.vsrna.backend.domain.balance;

import java.util.List;

public interface PointTransactionRepository {
    PointTransaction create(PointTransaction transaction);
    List<PointTransaction> list(PointTransactionQuery query);
}
