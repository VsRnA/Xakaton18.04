package com.prodforge.game.domain.gameevent;

import java.util.List;

public interface GameEventLogRepository {
    GameEventLog save(GameEventLog event);
    List<GameEventLog> list(GameEventLogQuery query);
}
