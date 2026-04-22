package com.vsrna.game.presentation.dto.round;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class RoundDto {

    public record SubmitSelectionRequest(
            @NotEmpty @Size(min = 1, max = 10) List<UUID> barrelIds
    ) {}

    public record BoostBarrelRequest(
            @NotNull UUID barrelId
    ) {}

    public record BarrelResponse(
            UUID id,
            String barrelCode,
            BigDecimal weight  // null до завершения раунда
    ) {}

    public record ParticipantScoreResponse(
            UUID participantId,
            boolean isBot,
            BigDecimal totalScore,
            int selectionCount,
            Integer rank
    ) {}

    public record RoundResultResponse(
            int roundNumber,
            String seedHash,
            String rawSeed,
            List<ParticipantScoreResponse> scores,
            UUID winnerId
    ) {}

    public record VerifyRoundResponse(
            String seedHash,
            String rawSeed,
            boolean valid
    ) {}

    public record GameHistoryResponse(
            UUID gameRoomId,
            UUID winnerUserId,
            boolean winnerIsBot,
            BigDecimal prizeAwarded,
            BigDecimal systemRevenue,
            java.time.Instant completedAt,
            String winCriteria
    ) {}

    public record ParticipantHistoryEntry(
            UUID participantId,
            UUID userId,
            boolean isBot,
            String displayName,
            boolean boostPurchased,
            BigDecimal totalScore,
            Integer rank,
            boolean isWinner
    ) {}

    public record GameHistoryDetailResponse(
            UUID gameRoomId,
            UUID winnerUserId,
            boolean winnerIsBot,
            @Schema(description = "Стоимость входа в игру (бонусных баллов)")
            BigDecimal entryFeeAmount,
            @Schema(description = "Сумма взносов реальных игроков (realPlayersCount × entryFeeAmount)")
            BigDecimal realPlayersRevenue,
            @Schema(description = "Выплаченный приз победителю. 0 если победил бот")
            BigDecimal prizeAwarded,
            @Schema(description = """
                    Баланс системы по итогам игры:
                    systemRevenue (% от призового фонда) + totalBoostBonuses − botCount × entryFeeAmount.
                    Положительный — бот победил, система в плюсе.
                    Отрицательный — реальный игрок победил, система покрыла часть приза из вложений в ботов.""")
            BigDecimal systemBalance,
            java.time.Instant completedAt,
            String winCriteria,
            int realPlayersCount,
            int botCount,
            @Schema(description = "Был ли буст доступен в этой игре (задаётся конфигурацией комнаты)")
            boolean boostAvailable,
            @Schema(description = "Сколько раз буст был куплен за игру (все раунды)")
            int boostUsedCount,
            @Schema(description = "Суммарный доход системы от продажи бустов в этой игре")
            BigDecimal totalBoostBonuses,
            List<ParticipantHistoryEntry> participants
    ) {}
}
