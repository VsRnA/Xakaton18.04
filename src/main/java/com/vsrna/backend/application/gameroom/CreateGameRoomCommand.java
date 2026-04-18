package com.vsrna.backend.application.gameroom;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateGameRoomCommand(
        UUID createdByUserId,
        int maxPlayers,
        BigDecimal entryFeeAmount,
        BigDecimal winnerPayoutPercentage,
        BigDecimal boostCostAmount,
        boolean isBoostEnabled
) {
}
