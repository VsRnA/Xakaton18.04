package com.vsrna.backend.domain.gameroom;

import java.math.BigDecimal;

public record GameRoomConfigPatch(
        Integer maxPlayers,
        BigDecimal entryFeeAmount,
        BigDecimal winnerPayoutPercentage,
        BigDecimal boostCostAmount,
        Boolean isBoostEnabled
) {
}
