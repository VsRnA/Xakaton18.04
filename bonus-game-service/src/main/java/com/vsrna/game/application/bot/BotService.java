package com.vsrna.game.application.bot;

import com.vsrna.game.domain.participant.GameParticipant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BotService {
    List<GameParticipant> createBotsForRoom(UUID roomId, int count, BigDecimal entryFeeAmount);
    void submitBotSelections(UUID roomId, int roundNumber);
}
