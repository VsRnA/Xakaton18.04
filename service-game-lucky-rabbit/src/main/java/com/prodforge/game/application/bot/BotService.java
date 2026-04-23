package com.prodforge.game.application.bot;

import com.prodforge.game.domain.participant.GameParticipant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BotService {
    List<GameParticipant> createBotsForRoom(UUID roomId, int count, BigDecimal entryFeeAmount);

    void submitBotSelections(UUID roomId, int roundNumber, boolean protectionMode,
                             Map<UUID, BigDecimal> barrelWeights);
}
