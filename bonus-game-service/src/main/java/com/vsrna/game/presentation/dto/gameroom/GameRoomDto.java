package com.vsrna.game.presentation.dto.gameroom;

import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.domain.gameroom.RepeatInterval;
import com.vsrna.game.domain.participant.ParticipantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GameRoomDto {

    public record CreateGameRoomRequest(
            @Min(2) @Max(10) int maxPlayers,
            @NotNull @DecimalMin("0.01") BigDecimal entryFeeAmount,
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal winnerPayoutPercentage,
            @NotNull @DecimalMin("0") BigDecimal boostCostAmount,
            boolean boostEnabled,
            @Min(1) @Max(10) int maxBarrelSelection,
            @Schema(description = "Время запуска комнаты (ISO-8601). Null — запуск немедленно", nullable = true)
            Instant scheduledStartAt,
            @Schema(description = "Период повторения: EVERY_30_MIN | EVERY_HOUR | EVERY_DAY | EVERY_WEEK | EVERY_MONTH. Null — без повторений", nullable = true)
            RepeatInterval repeatInterval,
            @Schema(description = "Подтвердить создание при наличии предупреждений. По умолчанию false", defaultValue = "false")
            boolean confirmWarnings
    ) {}

    public record ConfigResponse(
            int maxPlayers,
            BigDecimal entryFeeAmount,
            BigDecimal winnerPayoutPercentage,
            BigDecimal boostCostAmount,
            boolean isBoostEnabled,
            int maxBarrelSelection,
            @Schema(description = "Запланированное время запуска. Null если запуск немедленный", nullable = true)
            Instant scheduledStartAt,
            @Schema(description = "Период повторения. Null если без повторений", nullable = true)
            RepeatInterval repeatInterval
    ) {}

    public record GameRoomResponse(
            UUID id,
            GameRoomStatus status,
            int currentPlayerCount,
            BigDecimal prizePoolAmount,
            Instant createdAt,
            ConfigResponse config,
            @Schema(description = "Unix-миллисекунды окончания таймера ожидания. Null если таймер не активен", nullable = true)
            Long waitExpiresAt
    ) {}

    public record JoinRoomResponse(
            UUID participantId,
            BigDecimal reservedAmount,
            int currentPlayerCount,
            BigDecimal prizePoolAmount,
            @Schema(description = "Unix-миллисекунды окончания таймера ожидания. Null если комната заполнилась сразу", nullable = true)
            Long waitExpiresAt
    ) {}

    public record ParticipantResponse(
            UUID participantId,
            String displayName,
            boolean isBot,
            ParticipantStatus status,
            @Schema(description = "Вероятность победы в процентах на основе текущего числа игроков")
            double winProbabilityPercent
    ) {}

    // --- Оценка конфигурации ---

    public record ConfigWarningResponse(String code, String severity, String message) {}

    public record ConfigEvaluationResponse(
            BigDecimal projectedPrizePool,
            BigDecimal projectedSystemRevenue,
            double systemRevenuePercent,
            double playerExpectedValue,
            @Schema(description = "HIGH | MEDIUM | LOW")
            String attractivenessScore,
            List<ConfigWarningResponse> warnings
    ) {}

    public record CreateRoomResponse(GameRoomResponse room) {}

    // --- Следующая игра ---

    public record NextGameOption(
            @Schema(description = "SAME | SAFER | RISKIER")
            String type,
            GameRoomResponse room
    ) {}
}
