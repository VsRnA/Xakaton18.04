package com.vsrna.backend.domain.balance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"pointTransactions\"")
@Getter
@Setter
@NoArgsConstructor
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"userId\"", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "\"gameRoomId\"")
    private UUID gameRoomId;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "\"createdAt\"", nullable = false, updatable = false)
    private Instant createdAt;

    public PointTransaction(UUID userId, TransactionType type, BigDecimal amount, UUID gameRoomId, String description) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.gameRoomId = gameRoomId;
        this.description = description;
    }
}
