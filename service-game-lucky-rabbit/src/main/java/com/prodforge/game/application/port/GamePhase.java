package com.prodforge.game.application.port;

import java.util.UUID;

public enum GamePhase {
    FILL_BOTS("fill-bots"),
    RESOLVE_ROUND("resolve-round"),
    BOOST_DECISION_END("boost-decision-end"),
    FINALIZE_ROUND("finalize-round"),
    START_ROUND2("start-round2"),
    OPEN_ROOM("open-room");

    private final String prefix;

    GamePhase(String prefix) {
        this.prefix = prefix;
    }

    public String jobKey(UUID roomId) {
        return prefix + "-" + roomId;
    }

    public String jobKey(UUID roomId, int roundNumber) {
        return prefix + "-" + roomId + "-" + roundNumber;
    }
}
