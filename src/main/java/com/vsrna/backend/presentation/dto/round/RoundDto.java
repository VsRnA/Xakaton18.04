package com.vsrna.backend.presentation.dto.round;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class RoundDto {

    public record SubmitSelectionRequest(
            @NotEmpty @Size(min = 1, max = 5) List<UUID> barrelIds
    ) {}

    public record DiscardBarrelRequest(
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

    public record GameHistoryResponse(
            UUID gameRoomId,
            UUID winnerUserId,
            boolean winnerIsBot,
            BigDecimal prizeAwarded,
            BigDecimal systemRevenue,
            java.time.Instant completedAt,
            String winCriteria
    ) {}
}
