package com.prodforge.game.application.gameroom;

import com.prodforge.game.domain.gameroom.RepeatInterval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateGameRoomRequest(
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
