package com.vsrna.backend.domain.balance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PointTransaction {

    private UUID id;
    private UUID userId;
    private TransactionType type;
    private BigDecimal amount;
    private UUID gameRoomId;
    private String description;
    private String idempotencyKey;
    private Instant createdAt;

    public PointTransaction(UUID userId, TransactionType type, BigDecimal amount,
                            UUID gameRoomId, String description, String idempotencyKey) {
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.gameRoomId = gameRoomId;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
    }
}
