package com.vsrna.backend.presentation.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public sealed interface WsRoundEvent permits
        WsRoundEvent.RoundStarted,
        WsRoundEvent.PlayerSelected,
        WsRoundEvent.WeightsRevealed,
        WsRoundEvent.RoundCompleted {

    @Schema(description = "Раунд начался — клиент должен показать бочки для выбора")
    record RoundStarted(
            @Schema(example = "ROUND_STARTED") String type,
            @Schema(description = "Номер раунда (1 или 2)", example = "1") int roundNumber,
            @Schema(description = "Список ID бочек для выбора") List<String> barrelIds,
            @Schema(description = "Unix-миллисекунды окончания раунда", example = "1713520800000") long expiresAt
    ) implements WsRoundEvent {}

    @Schema(description = "Один из игроков сделал выбор бочек")
    record PlayerSelected(
            @Schema(example = "PLAYER_SELECTED") String type,
            @Schema(example = "1") int roundNumber,
            @Schema(description = "Сколько игроков уже выбрали", example = "2") int selectedCount,
            @Schema(description = "Всего игроков в комнате", example = "4") int totalPlayers
    ) implements WsRoundEvent {}

    @Schema(description = "RNG раскрыл веса бочек — открывается окно буста (5 сек)")
    record WeightsRevealed(
            @Schema(example = "WEIGHTS_REVEALED") String type,
            @Schema(example = "1") int roundNumber,
            @Schema(description = "Карта barrelId → вес (целое число)", example = "{\"uuid1\": 7, \"uuid2\": 3}") Map<String, Object> barrelWeights,
            @Schema(description = "SHA-256 хэш сида для верификации честности", example = "a3f2...") String seedHash,
            @Schema(description = "Raw hex сида (раскрывается для проверки)", example = "deadbeef...") String rawSeed,
            @Schema(description = "Unix-миллисекунды окончания окна буста", example = "1713520805000") long expiresAt
    ) implements WsRoundEvent {}

    @Schema(description = "Раунд завершён — объявлен победитель раунда")
    record RoundCompleted(
            @Schema(example = "ROUND_COMPLETED") String type,
            @Schema(example = "1") int roundNumber,
            @Schema(description = "ID участника-победителя раунда") String winnerId,
            @Schema(description = "Критерий победы: SCORE / SELECTION_COUNT / TIMESTAMP", example = "SCORE") String winCriteria
    ) implements WsRoundEvent {}
}
