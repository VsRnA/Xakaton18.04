package com.vsrna.game.fixtures;

import com.vsrna.game.domain.gameroom.GameRoomConfig;

import java.math.BigDecimal;
import java.util.UUID;

public final class GameRoomConfigFixtures {

    private GameRoomConfigFixtures() {}

    /** 4 игрока, вступительный взнос 100, выплата 90%, стоимость буста 50. */
    public static GameRoomConfig config(UUID roomId) {
        return new GameRoomConfig(roomId, 4, new BigDecimal("100"), new BigDecimal("90"),
                new BigDecimal("50"), true, 3);
    }

    public static GameRoomConfig configWith(UUID roomId, int maxPlayers, BigDecimal entryFee) {
        return new GameRoomConfig(roomId, maxPlayers, entryFee, new BigDecimal("90"),
                new BigDecimal("50"), true, 3);
    }
}
