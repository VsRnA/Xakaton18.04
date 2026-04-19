package com.vsrna.game.application.bot;

import com.vsrna.game.domain.participant.GameParticipant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BotService {
    List<GameParticipant> createBotsForRoom(UUID roomId, int count, BigDecimal entryFeeAmount);

    /**
     * Отправить выборы бочек за ботов.
     *
     * @param protectionMode если true — боты выбирают бочки с максимальным весом
     *                       (система защищает маржу), иначе — случайный выбор
     * @param barrelWeights  карта barrelId → weight (известна после раскрытия RNG)
     */
    void submitBotSelections(UUID roomId, int roundNumber, boolean protectionMode,
                             Map<UUID, BigDecimal> barrelWeights);
}
