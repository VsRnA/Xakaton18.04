package com.vsrna.game.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Порт для операций с балансом пользователей.
 * Application layer зависит от этого интерфейса — реализация в infrastructure.
 */
public interface BalancePort {
    void reserve(UUID userId, BigDecimal amount, UUID roomId);
    void release(UUID userId, BigDecimal amount, UUID roomId);
    void award(UUID userId, BigDecimal amount, UUID roomId);
    void deduct(UUID userId, BigDecimal amount, UUID roomId);
}
