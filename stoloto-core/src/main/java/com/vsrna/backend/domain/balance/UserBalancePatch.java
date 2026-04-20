package com.vsrna.backend.domain.balance;

import java.math.BigDecimal;

/**
 * Delta-семантика для атомарного обновления баланса.
 * Только одно поле должно быть non-null за раз.
 *
 * reserveDelta      — перевести из available в reserved (резервирование)
 * releaseDelta      — перевести из reserved в available (освобождение)
 * deductDelta       — списать из reserved (финальное списание)
 * directDeductDelta — списать напрямую из available (буст)
 * creditDelta       — зачислить в available (начисление)
 */
public record UserBalancePatch(
        BigDecimal reserveDelta,
        BigDecimal releaseDelta,
        BigDecimal deductDelta,
        BigDecimal directDeductDelta,
        BigDecimal creditDelta
) {
    public static UserBalancePatch reserve(BigDecimal amount) {
        return new UserBalancePatch(amount, null, null, null, null);
    }

    public static UserBalancePatch release(BigDecimal amount) {
        return new UserBalancePatch(null, amount, null, null, null);
    }

    public static UserBalancePatch deduct(BigDecimal amount) {
        return new UserBalancePatch(null, null, amount, null, null);
    }

    public static UserBalancePatch directDeduct(BigDecimal amount) {
        return new UserBalancePatch(null, null, null, amount, null);
    }

    public static UserBalancePatch credit(BigDecimal amount) {
        return new UserBalancePatch(null, null, null, null, amount);
    }
}
