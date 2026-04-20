package com.vsrna.game.domain.gameroom;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository {
    GameRoom create(GameRoom gameRoom);
    Optional<GameRoom> find(GameRoomQuery query);
    GameRoom get(GameRoomQuery query);
    GameRoom getForUpdate(GameRoomQuery query);
    List<GameRoom> list(GameRoomQuery query);
    GameRoom update(GameRoomQuery query, GameRoomPatch patch);
    void delete(GameRoomQuery query);
}
