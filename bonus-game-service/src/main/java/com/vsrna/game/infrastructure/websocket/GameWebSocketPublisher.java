package com.vsrna.game.infrastructure.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.domain.outbox.OutboxEvent;
import com.vsrna.game.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketPublisher implements GameNotifierPort {

    static final String TOPIC = "game.ws.event";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishRoomUpdate(UUID roomId, Map<String, Object> payload) {
        enqueue("GAME_ROOM", roomId.toString(), eventType(payload), "/topic/room/" + roomId, null, payload);
    }

    public void publishRoundEvent(UUID roomId, Map<String, Object> payload) {
        enqueue("ROUND", roomId.toString(), eventType(payload), "/topic/room/" + roomId + "/round", null, payload);
    }

    public void publishGameEvent(UUID roomId, Map<String, Object> payload) {
        enqueue("GAME", roomId.toString(), eventType(payload), "/topic/room/" + roomId + "/game", null, payload);
    }

    public void sendBarrelsToUser(String username, UUID roomId, Map<String, Object> payload) {
        enqueue("GAME_ROOM", roomId.toString(), eventType(payload), "/queue/game/" + roomId, username, payload);
    }

    public void publishRoomsUpdate(Map<String, Object> payload) {
        enqueue("GAME_ROOM", "global", eventType(payload), "/topic/rooms", null, payload);
    }

    public void publishBalanceUpdate(String userId, Map<String, Object> payload) {
        enqueue("BALANCE", userId, eventType(payload), "/queue/balance", userId, payload);
    }

    private void enqueue(String aggregateType, String aggregateId, String eventType,
                         String destination, String userId, Map<String, Object> payload) {
        try {
            Map<String, Object> envelope = new java.util.LinkedHashMap<>();
            envelope.put("destination", destination);
            if (userId != null) envelope.put("userId", userId);
            envelope.putAll(payload);
            String json = objectMapper.writeValueAsString(envelope);
            outboxRepository.save(new OutboxEvent(aggregateType, aggregateId, eventType, TOPIC, json));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WS event {}: {}", eventType, e.getMessage());
        }
    }

    private String eventType(Map<String, Object> payload) {
        Object type = payload.get("type");
        return type != null ? type.toString() : "UNKNOWN";
    }
}
