package com.prodforge.backend.presentation.dto.ws;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public sealed interface WsRoundEvent permits
        WsRoundEvent.RoundStarted,
        WsRoundEvent.PlayerSelected,
        WsRoundEvent.BoostDecisionStarted,
        WsRoundEvent.BoostWindowStarted,
        WsRoundEvent.RoundCompleted {

    @Schema(description = "Раунд начался — клиент должен показать бочки для выбора")
    record RoundStarted(
            @Schema(example = "ROUND_STARTED") String type,
            @Schema(description = "Номер раунда (1 или 2)", example = "1") int roundNumber,
            @ArraySchema(
                    schema = @Schema(description = "ID бочки (UUID)"),
                    minItems = 12, maxItems = 12,
                    arraySchema = @Schema(description = "Ровно 12 бочек раунда — фиксированный набор")
            )
            List<String> barrelIds,
            @Schema(description = "SHA-256 хэш сида — публикуется до раскрытия весов для верификации честности", example = "a3f2...") String seedHash,
            @Schema(description = "Unix-миллисекунды окончания раунда", example = "1713520800000") long expiresAt
    ) implements WsRoundEvent {}

    @Schema(description = "Один из игроков сделал выбор бочек")
    record PlayerSelected(
            @Schema(example = "PLAYER_SELECTED") String type,
            @Schema(example = "1") int roundNumber,
            @Schema(description = "Сколько игроков уже выбрали", example = "2") int selectedCount,
            @Schema(description = "Всего игроков в комнате", example = "4") int totalPlayers
    ) implements WsRoundEvent {}

    @Schema(description = "Раунд завершён, веса ещё не раскрыты — окно принятия решения о бусте (5 сек). Покупка буста в этой фазе недоступна.")
    record BoostDecisionStarted(
            @Schema(example = "BOOST_DECISION_STARTED") String type,
            @Schema(example = "1") int roundNumber,
            @Schema(description = "Unix-миллисекунды окончания фазы принятия решения", example = "1713520805000") long expiresAt
    ) implements WsRoundEvent {}

    @Schema(description = "Веса бочек раскрыты — отображаются эффекты буста (если куплен). Длится 5 сек, после чего начисляется финальный скор.")
    record BoostWindowStarted(
            @Schema(example = "BOOST_WINDOW_STARTED") String type,
            @Schema(description = "Номер раунда (1 или 2)", example = "1") int roundNumber,
            @Schema(description = "Карта barrelId → вес (число от -10 до +10)", example = "{\"uuid1\": 7, \"uuid2\": -3}") Map<String, Object> barrelWeights,
            @Schema(description = "SHA-256 хэш сида — совпадает с seedHash из ROUND_STARTED", example = "a3f2...") String seedHash,
            @Schema(description = "Raw hex сида — клиент может проверить SHA-256(rawSeed) == seedHash", example = "deadbeef...") String rawSeed,
            @Schema(description = "Карта participantId → эффект буста. Пустая если никто не купил буст.", example = "{\"uuid\": {\"barrelId\": \"...\", \"originalWeight\": -3, \"boostedWeight\": 3}}") Map<String, Object> boostEffects,
            @Schema(description = "Unix-миллисекунды окончания буст-окна", example = "1713520810000") long expiresAt
    ) implements WsRoundEvent {}

    @Schema(description = "Раунд завершён — объявлен победитель раунда")
    record RoundCompleted(
            @Schema(example = "ROUND_COMPLETED") String type,
            @Schema(example = "1") int roundNumber,
            @Schema(description = "ID участника-победителя раунда") String winnerId,
            @Schema(description = "Критерий победы: SCORE / SELECTION_COUNT / TIMESTAMP", example = "SCORE") String winCriteria,
            @ArraySchema(
                    schema = @Schema(description = "ID дисквалифицированного участника (UUID)"),
                    arraySchema = @Schema(description = "Участники, не сделавшие выбор в раунде — дисквалифицированы, ставка возвращена")
            )
            List<String> disqualifiedIds
    ) implements WsRoundEvent {}
}
