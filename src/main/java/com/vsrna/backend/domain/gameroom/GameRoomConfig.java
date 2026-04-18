package com.vsrna.backend.domain.gameroom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "\"gameRoomConfig\"")
@Getter
@Setter
@NoArgsConstructor
public class GameRoomConfig {

    @Id
    @Column(name = "\"gameRoomId\"", nullable = false)
    private UUID gameRoomId;

    @Column(name = "\"maxPlayers\"", nullable = false)
    private int maxPlayers;

    @Column(name = "\"entryFeeAmount\"", nullable = false, precision = 12, scale = 2)
    private BigDecimal entryFeeAmount;

    @Column(name = "\"winnerPayoutPercentage\"", nullable = false, precision = 5, scale = 2)
    private BigDecimal winnerPayoutPercentage;

    @Column(name = "\"boostCostAmount\"", nullable = false, precision = 12, scale = 2)
    private BigDecimal boostCostAmount;

    @Column(name = "\"isBoostEnabled\"", nullable = false)
    private boolean isBoostEnabled;

    public GameRoomConfig(UUID gameRoomId, int maxPlayers, BigDecimal entryFeeAmount,
                          BigDecimal winnerPayoutPercentage, BigDecimal boostCostAmount, boolean isBoostEnabled) {
        this.gameRoomId = gameRoomId;
        this.maxPlayers = maxPlayers;
        this.entryFeeAmount = entryFeeAmount;
        this.winnerPayoutPercentage = winnerPayoutPercentage;
        this.boostCostAmount = boostCostAmount;
        this.isBoostEnabled = isBoostEnabled;
    }
}
