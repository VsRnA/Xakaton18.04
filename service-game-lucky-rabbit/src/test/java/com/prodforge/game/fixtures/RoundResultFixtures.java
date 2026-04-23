package com.prodforge.game.fixtures;

import com.prodforge.game.domain.round.RoundResult;

import java.util.UUID;

public final class RoundResultFixtures {

    private RoundResultFixtures() {}

    public static RoundResult roundResult(UUID roomId, int roundNumber) {
        RoundResult r = new RoundResult(roomId, roundNumber);
        r.setId(UUID.randomUUID());
        return r;
    }
}
