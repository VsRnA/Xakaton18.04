package com.vsrna.game.domain.gameroom;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GameRoom {

    private UUID id;
    private GameRoomStatus status;
    private UUID createdByUserId;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant waitTimerExpiresAt;
    private int currentPlayerCount;
    private BigDecimal prizePoolAmount;

    public GameRoom(UUID createdByUserId, BigDecimal prizePoolAmount) {
        this.createdByUserId = createdByUserId;
        this.status = GameRoomStatus.WAITING;
        this.currentPlayerCount = 0;
        this.prizePoolAmount = prizePoolAmount;
    }
}
