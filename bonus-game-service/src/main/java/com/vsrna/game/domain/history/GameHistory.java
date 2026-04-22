package com.vsrna.game.domain.history;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GameHistory {

    private UUID id;
    private UUID gameRoomId;
    private UUID winnerUserId;
    private boolean winnerIsBot;
    private BigDecimal prizeAwarded;
    private BigDecimal systemRevenue;
    private Instant completedAt;
    private String winCriteria;
    private String summaryJson;

    private int realPlayersCount;
    private int botCount;
    private BigDecimal realPlayersRevenue;
    private BigDecimal boostRevenue;
    private int boostUsedCount;
    private boolean winnerUsedBoost;
    private BigDecimal entryFeeAmount;
    private boolean boostAvailable;

    public GameHistory(UUID gameRoomId, UUID winnerUserId, boolean winnerIsBot,
                       BigDecimal prizeAwarded, BigDecimal systemRevenue, String winCriteria,
                       int realPlayersCount, int botCount, BigDecimal realPlayersRevenue,
                       BigDecimal boostRevenue, int boostUsedCount, boolean winnerUsedBoost,
                       BigDecimal entryFeeAmount, boolean boostAvailable) {
        this.gameRoomId = gameRoomId;
        this.winnerUserId = winnerUserId;
        this.winnerIsBot = winnerIsBot;
        this.prizeAwarded = prizeAwarded;
        this.systemRevenue = systemRevenue;
        this.winCriteria = winCriteria;
        this.realPlayersCount = realPlayersCount;
        this.botCount = botCount;
        this.realPlayersRevenue = realPlayersRevenue;
        this.boostRevenue = boostRevenue;
        this.boostUsedCount = boostUsedCount;
        this.winnerUsedBoost = winnerUsedBoost;
        this.entryFeeAmount = entryFeeAmount;
        this.boostAvailable = boostAvailable;
    }

    /**
     * Баланс системы по итогам игры:
     *   + systemRevenue  — что система оставила себе из призового фонда (10% и т.п.)
     *   + boostRevenue   — чистый доход от продажи бустов
     *   - botCost        — сколько система вложила в ботов (botCount × entryFeeAmount)
     */
    public BigDecimal getSystemBalance() {
        BigDecimal sysRev = systemRevenue != null ? systemRevenue : BigDecimal.ZERO;
        BigDecimal boost = boostRevenue != null ? boostRevenue : BigDecimal.ZERO;
        BigDecimal botCost = entryFeeAmount != null
                ? entryFeeAmount.multiply(BigDecimal.valueOf(botCount))
                : BigDecimal.ZERO;
        return sysRev.add(boost).subtract(botCost);
    }
}
