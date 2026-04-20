package com.vsrna.game.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * Порт для публикации игровых событий клиентам (WebSocket / real-time уведомления).
 * Application layer не зависит от WebSocket/STOMP напрямую.
 */
public interface GameNotifierPort {
    void publishRoomsUpdate(Map<String, Object> payload);
    void publishRoomUpdate(UUID roomId, Map<String, Object> payload);
    void publishRoundEvent(UUID roomId, Map<String, Object> payload);
    void publishGameEvent(UUID roomId, Map<String, Object> payload);
}
