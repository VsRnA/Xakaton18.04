package com.prodforge.game.application.round.history;

import com.prodforge.game.domain.history.GameHistory;

import java.util.List;

public record GameHistoryDetails(
        GameHistory history,
        List<ParticipantHistoryEntry> participants
) {}
