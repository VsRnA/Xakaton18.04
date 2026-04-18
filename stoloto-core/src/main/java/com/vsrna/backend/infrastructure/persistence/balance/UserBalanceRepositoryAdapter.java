package com.vsrna.backend.infrastructure.persistence.balance;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.balance.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserBalanceRepositoryAdapter implements UserBalanceRepository {

    private final UserBalanceJpaRepository jpa;

    @Override
    public UserBalance create(UserBalance userBalance) {
        return jpa.save(userBalance);
    }

    @Override
    public Optional<UserBalance> find(UserBalanceQuery query) {
        return jpa.findById(query.userId());
    }

    @Override
    public UserBalance get(UserBalanceQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("UserBalance", query.userId().toString()));
    }

    @Override
    @Transactional
    public UserBalance update(UserBalanceQuery query, UserBalancePatch patch) {
        UserBalance balance = get(query);
        Instant now = Instant.now();
        int rows;

        if (patch.reserveDelta() != null) {
            rows = jpa.reserve(balance.getUserId(), patch.reserveDelta(), balance.getVersion(), now);
            assertUpdated(rows, "Insufficient available balance for reserve");
        } else if (patch.releaseDelta() != null) {
            rows = jpa.release(balance.getUserId(), patch.releaseDelta(), balance.getVersion(), now);
            assertUpdated(rows, "Insufficient reserved balance for release");
        } else if (patch.deductDelta() != null) {
            rows = jpa.deductReserved(balance.getUserId(), patch.deductDelta(), balance.getVersion(), now);
            assertUpdated(rows, "Insufficient reserved balance for deduct");
        } else if (patch.directDeductDelta() != null) {
            rows = jpa.deduct(balance.getUserId(), patch.directDeductDelta(), balance.getVersion(), now);
            assertUpdated(rows, "Insufficient available balance for direct deduct");
        } else if (patch.creditDelta() != null) {
            rows = jpa.credit(balance.getUserId(), patch.creditDelta(), balance.getVersion(), now);
            assertUpdated(rows, "Credit failed (version conflict)");
        } else {
            throw ApiException.badRequest("UserBalancePatch has no delta set");
        }

        return get(query);
    }

    private void assertUpdated(int rows, String message) {
        if (rows == 0) {
            throw ApiException.badRequest(message);
        }
    }
}
