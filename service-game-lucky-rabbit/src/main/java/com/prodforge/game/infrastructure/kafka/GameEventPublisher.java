package com.prodforge.game.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.domain.outbox.OutboxEvent;
import com.prodforge.game.domain.outbox.OutboxEventRepository;
import com.prodforge.game.infrastructure.kafka.event.BalanceCommandEvent;
import com.prodforge.game.infrastructure.kafka.event.GameEntryReservedEvent;
import com.prodforge.game.infrastructure.kafka.event.GameFinishedEvent;
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
    private static final String TOPIC_BALANCE_COMMAND = "balance.command";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publishGameFinished(UUID roomId, UUID winnerId, boolean winnerIsBot,
                                    BigDecimal prizePool, BigDecimal prizeAwarded,
                                    BigDecimal systemRevenue, String winCriteria) {
        GameFinishedEvent event = new GameFinishedEvent(
                GameFinishedEvent.CURRENT_VERSION,
                roomId, winnerId, winnerIsBot, prizePool, prizeAwarded, systemRevenue, winCriteria);
        enqueue("GAME", roomId.toString(), "GAME_FINISHED", TOPIC_FINISHED, event);
    }

    public void publishEntryReserved(UUID userId, UUID roomId, BigDecimal amount) {
        GameEntryReservedEvent event = new GameEntryReservedEvent(GameEntryReservedEvent.CURRENT_VERSION, userId, roomId, amount);
        enqueue("GAME_ROOM", roomId.toString(), "ENTRY_RESERVED", TOPIC_ENTRY_RESERVED, event);
    }

    @Override
    public void publishBalanceReserve(UUID userId, BigDecimal amount, UUID roomId) {
        enqueue("BALANCE", userId.toString(), "BALANCE_RESERVE", TOPIC_BALANCE_COMMAND,
                new BalanceCommandEvent(BalanceCommandEvent.CURRENT_VERSION, "RESERVE", userId, amount, roomId));
    }

    @Override
    public void publishBalanceRelease(UUID userId, BigDecimal amount, UUID roomId) {
        enqueue("BALANCE", userId.toString(), "BALANCE_RELEASE", TOPIC_BALANCE_COMMAND,
                new BalanceCommandEvent(BalanceCommandEvent.CURRENT_VERSION, "RELEASE", userId, amount, roomId));
    }

    @Override
    public void publishBalanceAward(UUID userId, BigDecimal amount, UUID roomId) {
        enqueue("BALANCE", userId.toString(), "BALANCE_AWARD", TOPIC_BALANCE_COMMAND,
                new BalanceCommandEvent(BalanceCommandEvent.CURRENT_VERSION, "AWARD", userId, amount, roomId));
    }

    @Override
    public void publishBalanceDeduct(UUID userId, BigDecimal amount, UUID roomId) {
        enqueue("BALANCE", userId.toString(), "BALANCE_DEDUCT", TOPIC_BALANCE_COMMAND,
                new BalanceCommandEvent(BalanceCommandEvent.CURRENT_VERSION, "DEDUCT", userId, amount, roomId));
    }

    @Override
    public void publishBalanceDeductReserved(UUID userId, BigDecimal amount, UUID roomId) {
        enqueue("BALANCE", userId.toString(), "BALANCE_DEDUCT_RESERVED", TOPIC_BALANCE_COMMAND,
                new BalanceCommandEvent(BalanceCommandEvent.CURRENT_VERSION, "DEDUCT_RESERVED", userId, amount, roomId));
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
