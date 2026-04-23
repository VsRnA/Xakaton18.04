package com.prodforge.game.domain.outbox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    private UUID id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String topic;
    private String payload;
    private OutboxStatus status;
    private Instant createdAt;
    private Instant processedAt;

    public OutboxEvent(String aggregateType, String aggregateId, String eventType,
                       String topic, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
    }
}
