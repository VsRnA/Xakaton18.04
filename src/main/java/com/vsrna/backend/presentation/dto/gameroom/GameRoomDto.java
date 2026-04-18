package com.vsrna.backend.presentation.dto.gameroom;

import com.vsrna.backend.domain.gameroom.GameRoomStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class GameRoomDto {

    public record CreateGameRoomRequest(
            @Min(2) @Max(10) int maxPlayers,
            @NotNull @DecimalMin("0.01") BigDecimal entryFeeAmount,
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal winnerPayoutPercentage,
            @NotNull @DecimalMin("0") BigDecimal boostCostAmount,
            boolean boostEnabled
    ) {}

    public record ConfigResponse(
            int maxPlayers,
            BigDecimal entryFeeAmount,
            BigDecimal winnerPayoutPercentage,
            BigDecimal boostCostAmount,
            boolean isBoostEnabled
    ) {}

    public record GameRoomResponse(
            UUID id,
            GameRoomStatus status,
            int currentPlayerCount,
            BigDecimal prizePoolAmount,
            Instant createdAt,
            ConfigResponse config
    ) {}

    public record JoinRoomResponse(
            UUID participantId,
            BigDecimal reservedAmount,
            int currentPlayerCount,
            BigDecimal prizePoolAmount
    ) {}
}
