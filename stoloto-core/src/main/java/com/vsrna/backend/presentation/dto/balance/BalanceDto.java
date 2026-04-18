package com.vsrna.backend.presentation.dto.balance;

import java.math.BigDecimal;

public class BalanceDto {

    public record UserBalanceResponse(
            BigDecimal available,
            BigDecimal reserved
            // total не хранится — вычисляется на клиенте: available + reserved
    ) {}
}
