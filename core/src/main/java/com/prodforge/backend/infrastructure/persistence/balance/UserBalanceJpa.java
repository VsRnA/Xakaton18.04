package com.prodforge.backend.infrastructure.persistence.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "UserBalance")
@Table(name = "\"userBalances\"")
@Getter
@Setter
@NoArgsConstructor
class UserBalanceJpa {

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
}
