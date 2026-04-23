package com.prodforge.backend.domain.balance;

import java.util.UUID;

public record UserBalanceQuery(UUID userId) {
    public static UserBalanceQuery byUser(UUID userId) {
        return new UserBalanceQuery(userId);
    }
}
