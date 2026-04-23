package com.prodforge.game.domain.gameroom;

import java.util.List;
import java.util.UUID;

public interface GameRoomConfigRepository {
    GameRoomConfig create(GameRoomConfig config);
    GameRoomConfig get(GameRoomConfigQuery query);
    List<GameRoomConfig> listByRoomIds(List<UUID> roomIds);
}
