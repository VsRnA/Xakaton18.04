package com.vsrna.backend.domain.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"userBalances\"")
@Getter
@Setter
@NoArgsConstructor
public class UserBalance {

    @Id
    @Column(name = "\"userId\"", nullable = false)
    private UUID userId;

    @Column(name = "available", nullable = false, precision = 12, scale = 2)
    private BigDecimal available;

    @Column(name = "reserved", nullable = false, precision = 12, scale = 2)
    private BigDecimal reserved;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "\"updatedAt\"", nullable = false)
    private Instant updatedAt;

    public UserBalance(UUID userId, BigDecimal available) {
        this.userId = userId;
        this.available = available;
        this.reserved = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }
}
