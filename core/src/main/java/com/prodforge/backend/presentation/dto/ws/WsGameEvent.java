package com.prodforge.backend.presentation.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public sealed interface WsGameEvent permits
        WsGameEvent.FinalistsAnnounced,
        WsGameEvent.GameFinished {

    @Schema(description = "Объявлены финалисты — два лучших игрока раунда 1 переходят в раунд 2")
    record FinalistsAnnounced(
            @Schema(example = "FINALISTS_ANNOUNCED") String type,
            @Schema(description = "IDs двух участников-финалистов") List<String> finalistIds,
            @Schema(description = "Критерий победы в раунде 1", example = "SCORE") String winCriteria
    ) implements WsGameEvent {}

    @Schema(description = "Игра завершена — приз распределён")
    record GameFinished(
            @Schema(example = "GAME_FINISHED") String type,
            @Schema(description = "ID участника-победителя") String winnerParticipantId,
            @Schema(description = "true если победил бот") boolean winnerIsBot,
            @Schema(description = "Сумма приза победителю", example = "810.00") String prizeAwarded,
            @Schema(description = "Комиссия системы", example = "90.00") String systemRevenue,
            @Schema(description = "Критерий победы", example = "SCORE") String winCriteria
    ) implements WsGameEvent {}
}
