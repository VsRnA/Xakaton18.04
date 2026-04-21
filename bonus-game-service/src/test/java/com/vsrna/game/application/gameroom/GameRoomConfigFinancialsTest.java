package com.vsrna.game.application.gameroom;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomConfigFinancialsTest {

    private final GameRoomConfigValidator validator = new GameRoomConfigValidator();

    @Test
    void prizePoolAndSystemRevenue_calculatedCorrectly() {
        // 4 игрока × 100 баллов × 90% → приз 360, системный доход 40
        var result = evaluate(4, "100", "90");

        assertThat(result.projectedPrizePool()).isEqualByComparingTo(new BigDecimal("360.00"));
        assertThat(result.projectedSystemRevenue()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.systemRevenuePercent()).isEqualTo(10.0);
    }

    @Test
    void playerExpectedValue_negativeForLosingPlayers() {
        // prizePool / maxPlayers - entryFee = 360/4 - 100 = -10
        var result = evaluate(4, "100", "90");

        assertThat(result.playerExpectedValue()).isEqualTo(-10.0);
    }

    private ConfigEvaluationResult evaluate(int maxPlayers, String entryFee, String payoutPct) {
        return validator.evaluate(maxPlayers, new BigDecimal(entryFee), new BigDecimal(payoutPct),
                new BigDecimal("50"), true, 3);
    }
}
