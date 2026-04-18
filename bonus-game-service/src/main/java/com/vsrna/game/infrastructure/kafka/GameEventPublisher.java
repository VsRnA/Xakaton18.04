package com.vsrna.game.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.domain.outbox.OutboxEvent;
import com.vsrna.game.domain.outbox.OutboxEventRepository;
import com.vsrna.game.infrastructure.kafka.event.GameEntryReservedEvent;
import com.vsrna.game.infrastructure.kafka.event.GameFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventPublisher implements GameEventPort {

    private static final String TOPIC_FINISHED = "game.finished";
    private static final String TOPIC_ENTRY_RESERVED = "game.entry.reserved";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishGameFinished(UUID roomId, UUID winnerId, boolean winnerIsBot,
                                    BigDecimal prizePool, BigDecimal prizeAwarded,
                                    BigDecimal systemRevenue, String winCriteria) {
        GameFinishedEvent event = new GameFinishedEvent(
                roomId, winnerId, winnerIsBot, prizePool, prizeAwarded, systemRevenue, winCriteria);
        enqueue("GAME", roomId.toString(), "GAME_FINISHED", TOPIC_FINISHED, event);
    }

    public void publishEntryReserved(UUID userId, UUID roomId, BigDecimal amount) {
        GameEntryReservedEvent event = new GameEntryReservedEvent(userId, roomId, amount);
        enqueue("GAME_ROOM", roomId.toString(), "ENTRY_RESERVED", TOPIC_ENTRY_RESERVED, event);
    }

    private void enqueue(String aggregateType, String aggregateId, String eventType,
                         String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(new OutboxEvent(aggregateType, aggregateId, eventType, topic, json));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}: {}", eventType, e.getMessage());
        }
    }
}
