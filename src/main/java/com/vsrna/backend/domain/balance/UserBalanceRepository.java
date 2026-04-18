package com.vsrna.backend.domain.balance;

import java.util.Optional;

public interface UserBalanceRepository {
    UserBalance create(UserBalance userBalance);
    Optional<UserBalance> find(UserBalanceQuery query);
    UserBalance get(UserBalanceQuery query);
    UserBalance update(UserBalanceQuery query, UserBalancePatch patch);
}
