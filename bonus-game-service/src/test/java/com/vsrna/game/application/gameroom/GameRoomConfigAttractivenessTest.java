package com.vsrna.game.application.gameroom;

import com.vsrna.game.application.gameroom.config.ConfigEvaluationResult;
import com.vsrna.game.application.gameroom.config.GameRoomConfigValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomConfigAttractivenessTest {

    private final GameRoomConfigValidator validator = new GameRoomConfigValidator();

    @Test
    void highPayoutAffordableBoost_attractivenessIsHigh() {
        var result = evaluate("80", "40");

        assertThat(result.attractivenessScore()).isEqualTo("HIGH");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void lowPayout_attractivenessIsLow() {
        var result = evaluate("40", "50");

        assertThat(result.attractivenessScore()).isEqualTo("LOW");
    }

    private ConfigEvaluationResult evaluate(String payoutPct, String boostCost) {
        return validator.evaluate(4, new BigDecimal("100"), new BigDecimal(payoutPct),
                new BigDecimal(boostCost), true, 3);
    }
}
