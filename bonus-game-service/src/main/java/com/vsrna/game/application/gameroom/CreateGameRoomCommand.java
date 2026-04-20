package com.vsrna.game.application.gameroom;

import com.vsrna.game.domain.gameroom.RepeatInterval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateGameRoomCommand(
        UUID createdByUserId,
        int maxPlayers,
        BigDecimal entryFeeAmount,
        BigDecimal winnerPayoutPercentage,
        BigDecimal boostCostAmount,
        boolean isBoostEnabled,
        int maxBarrelSelection,
        Instant scheduledStartAt,
        RepeatInterval repeatInterval
) {
}
