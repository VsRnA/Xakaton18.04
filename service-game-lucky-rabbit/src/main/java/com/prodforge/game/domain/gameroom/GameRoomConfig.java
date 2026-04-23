package com.prodforge.game.domain.gameroom;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GameRoomConfig {

    private UUID gameRoomId;
    private int maxPlayers;
    private BigDecimal entryFeeAmount;
    private BigDecimal winnerPayoutPercentage;
    private BigDecimal boostCostAmount;
    private boolean isBoostEnabled;
    private int maxBarrelSelection;
    private Instant scheduledStartAt;
    private RepeatInterval repeatInterval;

    public GameRoomConfig(UUID gameRoomId, int maxPlayers, BigDecimal entryFeeAmount,
                          BigDecimal winnerPayoutPercentage, BigDecimal boostCostAmount, boolean isBoostEnabled,
                          int maxBarrelSelection) {
        this.gameRoomId = gameRoomId;
        this.maxPlayers = maxPlayers;
        this.entryFeeAmount = entryFeeAmount;
        this.winnerPayoutPercentage = winnerPayoutPercentage;
        this.boostCostAmount = boostCostAmount;
        this.isBoostEnabled = isBoostEnabled;
        this.maxBarrelSelection = maxBarrelSelection;
    }

    public GameRoomConfig(UUID gameRoomId, int maxPlayers, BigDecimal entryFeeAmount,
                          BigDecimal winnerPayoutPercentage, BigDecimal boostCostAmount, boolean isBoostEnabled,
                          int maxBarrelSelection, Instant scheduledStartAt, RepeatInterval repeatInterval) {
        this(gameRoomId, maxPlayers, entryFeeAmount, winnerPayoutPercentage, boostCostAmount, isBoostEnabled, maxBarrelSelection);
        this.scheduledStartAt = scheduledStartAt;
        this.repeatInterval = repeatInterval;
    }
}
