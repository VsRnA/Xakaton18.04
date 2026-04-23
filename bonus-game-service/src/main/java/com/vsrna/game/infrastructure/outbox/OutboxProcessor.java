package com.vsrna.game.infrastructure.outbox;

import com.vsrna.game.domain.outbox.OutboxEvent;
import com.vsrna.game.domain.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    private final AtomicInteger pendingGauge = new AtomicInteger(0);

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("game_outbox_pending", pendingGauge, AtomicInteger::get)
                .description("Outbox events pending in last processing batch (equals batch-size if backlog is growing)")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    @Transactional
    public void process() {
        List<OutboxEvent> pending = outboxRepository.findPending(batchSize);
        pendingGauge.set(pending.size());
        if (pending.isEmpty()) return;

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload());
                outboxRepository.markProcessed(event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}, type={}: {}",
                        event.getId(), event.getEventType(), e.getMessage());
                outboxRepository.markFailed(event.getId());
            }
        }
    }
}
