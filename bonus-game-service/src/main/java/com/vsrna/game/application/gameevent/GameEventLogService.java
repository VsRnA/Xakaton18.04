package com.vsrna.game.application.gameevent;

import com.vsrna.game.domain.gameevent.GameEventLog;

import java.util.List;
import java.util.UUID;

public interface GameEventLogService {
    void log(UUID roomId, String eventType, String details);
    List<GameEventLog> getEvents(UUID roomId);
}
