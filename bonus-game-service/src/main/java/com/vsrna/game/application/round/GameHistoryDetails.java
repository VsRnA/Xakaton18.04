package com.vsrna.game.application.round;

import com.vsrna.game.domain.history.GameHistory;

import java.util.List;

public record GameHistoryDetails(
        GameHistory history,
        List<ParticipantHistoryEntry> participants
) {}
