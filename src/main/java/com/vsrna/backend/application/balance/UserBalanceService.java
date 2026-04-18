package com.vsrna.backend.application.balance;

import com.vsrna.backend.domain.balance.UserBalance;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserBalanceService {
    UserBalance getBalance(UUID userId);
    void reservePoints(UUID userId, BigDecimal amount, UUID gameRoomId);
    void deductReserved(UUID userId, BigDecimal amount, UUID gameRoomId);
    void deductPoints(UUID userId, BigDecimal amount, UUID gameRoomId);
    void creditPoints(UUID userId, BigDecimal amount, UUID gameRoomId);
}
