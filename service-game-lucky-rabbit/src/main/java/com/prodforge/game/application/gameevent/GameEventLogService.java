package com.prodforge.game.application.gameevent;

import com.prodforge.game.domain.gameevent.GameEventLog;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GameEventLogService {
    void log(UUID roomId, String eventType, Map<String, Object> details);
    List<GameEventLog> getEvents(UUID roomId);
}
