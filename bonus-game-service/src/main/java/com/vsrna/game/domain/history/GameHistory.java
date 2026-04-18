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

    public GameHistory(UUID gameRoomId, UUID winnerUserId, boolean winnerIsBot,
                       BigDecimal prizeAwarded, BigDecimal systemRevenue, String winCriteria) {
        this.gameRoomId = gameRoomId;
        this.winnerUserId = winnerUserId;
        this.winnerIsBot = winnerIsBot;
        this.prizeAwarded = prizeAwarded;
        this.systemRevenue = systemRevenue;
        this.winCriteria = winCriteria;
    }
}
