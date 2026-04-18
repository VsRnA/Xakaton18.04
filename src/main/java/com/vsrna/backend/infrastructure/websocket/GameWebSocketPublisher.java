package com.vsrna.backend.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GameWebSocketPublisher {

    private final SimpMessagingTemplate messaging;

    /**
     * Публикует обновление состояния комнаты всем подписчикам.
     * Топик: /topic/room/{roomId}
     */
    public void publishRoomUpdate(UUID roomId, Object payload) {
        messaging.convertAndSend("/topic/room/" + roomId, payload);
    }

    /**
     * Публикует событие раунда (ROUND_STARTED, WEIGHTS_REVEALED, ROUND_COMPLETED).
     * Топик: /topic/room/{roomId}/round
     */
    public void publishRoundEvent(UUID roomId, Object payload) {
        messaging.convertAndSend("/topic/room/" + roomId + "/round", payload);
    }

    /**
     * Публикует игровое событие (FINALISTS_ANNOUNCED, GAME_FINISHED).
     * Топик: /topic/room/{roomId}/game
     */
    public void publishGameEvent(UUID roomId, Object payload) {
        messaging.convertAndSend("/topic/room/" + roomId + "/game", payload);
    }

    /**
     * Отправляет перемешанные бочки конкретному пользователю.
     * Топик: /user/queue/game/{roomId}
     */
    public void sendBarrelsToUser(String username, UUID roomId, Object payload) {
        messaging.convertAndSendToUser(username, "/queue/game/" + roomId, payload);
    }
}
