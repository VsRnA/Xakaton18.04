package com.vsrna.backend.application.balance;

import com.vsrna.backend.domain.balance.UserBalance;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserBalanceService {
    UserBalance getBalance(UUID userId);
    /** Резервирует amount из available → reserved (вход в игру). */
    void reservePoints(UUID userId, BigDecimal amount, UUID gameRoomId);
    /** Возвращает amount из reserved → available (игрок выбыл, деньги возвращены). */
    void returnReservedPoints(UUID userId, BigDecimal amount, UUID gameRoomId);
    /** Списывает amount из reserved навсегда (финальное списание платежа). */
    void deductReserved(UUID userId, BigDecimal amount, UUID gameRoomId);
    /** Списывает amount напрямую из available (покупка буста). */
    void deductPoints(UUID userId, BigDecimal amount, UUID gameRoomId);
    /** Зачисляет amount в available (выплата приза). */
    void creditPoints(UUID userId, BigDecimal amount, UUID gameRoomId);
}
