package com.prodforge.backend.presentation.dto.balance;

import java.math.BigDecimal;

public class BalanceDto {

    public record UserBalanceResponse(
            BigDecimal available,
            BigDecimal reserved
    ) {}
}
