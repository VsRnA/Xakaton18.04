package com.prodforge.backend.domain.balance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserBalance {

    private UUID userId;
    private BigDecimal available;
    private BigDecimal reserved;
    private Instant updatedAt;

    public UserBalance(UUID userId, BigDecimal available) {
        this.userId = userId;
        this.available = available;
        this.reserved = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }
}
