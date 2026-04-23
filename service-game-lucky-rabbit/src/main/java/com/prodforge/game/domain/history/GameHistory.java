package com.prodforge.game.domain.history;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
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

    public double getBotFillRate() {
        int total = realPlayersCount + botCount;
        return total > 0 ? (double) botCount / total : 0.0;
    }

    public BigDecimal getSystemBalance() {
        BigDecimal sysRev = systemRevenue != null ? systemRevenue : BigDecimal.ZERO;
        BigDecimal boost = boostRevenue != null ? boostRevenue : BigDecimal.ZERO;
        BigDecimal botCost = entryFeeAmount != null
                ? entryFeeAmount.multiply(BigDecimal.valueOf(botCount))
                : BigDecimal.ZERO;
        return sysRev.add(boost).subtract(botCost);
    }
}
