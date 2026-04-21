package com.vsrna.game.application.gameroom;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomConfigWarningsTest {

    private final GameRoomConfigValidator validator = new GameRoomConfigValidator();

    @Test
    void payoutBelow50_warnsLowPlayerPayout() {
        var result = evaluate("40", "50", true, 4, 3);

        assertThat(warningCodes(result)).contains("LOW_PLAYER_PAYOUT");
    }

    @Test
    void payoutAbove95_warnsLowOrganizerRevenue() {
        var result = evaluate("96", "50", true, 4, 3);

        assertThat(warningCodes(result)).contains("LOW_ORGANIZER_REVENUE");
    }

    @Test
    void boostDisabledWithCostSet_errorsInconsistentConfig() {
        var result = evaluate("80", "50", false, 4, 3);

        assertThat(errorCodes(result)).contains("BOOST_CONFIG_INCONSISTENT");
        assertThat(result.attractivenessScore()).isEqualTo("LOW");
    }

    @Test
    void boostDisabledWithZeroCost_noWarning() {
        var result = evaluate("80", "0", false, 4, 3);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.attractivenessScore()).isEqualTo("HIGH");
    }

    @Test
    void boostCostExceedsEntryFee_warnsBoostTooExpensive() {
        var result = evaluate("80", "150", true, 4, 3);

        assertThat(warningCodes(result)).contains("BOOST_TOO_EXPENSIVE");
    }

    @Test
    void maxBarrelSelectionOne_warnsLowSelectionChoice() {
        var result = evaluate("80", "40", true, 4, 1);

        assertThat(warningCodes(result)).contains("LOW_SELECTION_CHOICE");
    }

    @Test
    void twoPlayers_infoSmallRoom() {
        var result = evaluate("80", "40", true, 2, 3);

        assertThat(infoCodes(result)).contains("SMALL_ROOM");
    }

    private ConfigEvaluationResult evaluate(String payoutPct, String boostCost,
                                            boolean boostEnabled, int maxPlayers, int maxBarrelSelection) {
        return validator.evaluate(maxPlayers, new BigDecimal("100"), new BigDecimal(payoutPct),
                new BigDecimal(boostCost), boostEnabled, maxBarrelSelection);
    }

    private List<String> warningCodes(ConfigEvaluationResult result) {
        return result.warnings().stream()
                .filter(w -> "WARN".equals(w.severity()))
                .map(ConfigWarning::code)
                .toList();
    }

    private List<String> errorCodes(ConfigEvaluationResult result) {
        return result.warnings().stream()
                .filter(w -> "ERROR".equals(w.severity()))
                .map(ConfigWarning::code)
                .toList();
    }

    private List<String> infoCodes(ConfigEvaluationResult result) {
        return result.warnings().stream()
                .filter(w -> "INFO".equals(w.severity()))
                .map(ConfigWarning::code)
                .toList();
    }
}
