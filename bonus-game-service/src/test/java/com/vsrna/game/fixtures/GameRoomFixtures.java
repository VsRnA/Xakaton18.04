package com.vsrna.game.fixtures;

import com.vsrna.game.domain.gameroom.GameRoom;
import com.vsrna.game.domain.gameroom.GameRoomStatus;

import java.math.BigDecimal;
import java.util.UUID;

public final class GameRoomFixtures {

    private GameRoomFixtures() {}

    public static GameRoom waitingRoom(UUID roomId) {
        GameRoom room = new GameRoom(UUID.randomUUID(), BigDecimal.ZERO);
        room.setId(roomId);
        room.setStatus(GameRoomStatus.WAITING);
        room.setCurrentPlayerCount(0);
        room.setPrizePoolAmount(BigDecimal.ZERO);
        return room;
    }

    public static GameRoom waitingRoomWith(UUID roomId, int players, BigDecimal prize) {
        GameRoom room = waitingRoom(roomId);
        room.setCurrentPlayerCount(players);
        room.setPrizePoolAmount(prize);
        return room;
    }
}
