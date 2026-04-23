package com.prodforge.game.infrastructure.outbox;

import com.prodforge.game.domain.outbox.OutboxEvent;
import com.prodforge.game.domain.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock OutboxEventRepository outboxRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Spy SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxProcessor(outboxRepository, kafkaTemplate, meterRegistry);
        processor.initMetrics();
        ReflectionTestUtils.setField(processor, "batchSize", 50);
    }

    @Test
    void process_emptyBatch_doesNothing() {
        when(outboxRepository.findPending(50)).thenReturn(List.of());

        processor.process();

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(outboxRepository, never()).markProcessed(any());
        verify(outboxRepository, never()).markFailed(any());
    }

    @Test
    void process_happyPath_publishesAndMarkProcessed() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        OutboxEvent e1 = event(id1, "topic-a", "key-1", "{\"v\":1}");
        OutboxEvent e2 = event(id2, "topic-b", "key-2", "{\"v\":2}");

        when(outboxRepository.findPending(50)).thenReturn(List.of(e1, e2));

        processor.process();

        InOrder order = inOrder(kafkaTemplate, outboxRepository);
        order.verify(kafkaTemplate).send("topic-a", "key-1", "{\"v\":1}");
        order.verify(outboxRepository).markProcessed(id1);
        order.verify(kafkaTemplate).send("topic-b", "key-2", "{\"v\":2}");
        order.verify(outboxRepository).markProcessed(id2);
        verify(outboxRepository, never()).markFailed(any());
    }

    @Test
    void process_kafkaFailure_marksEventFailedAndContinuesToNext() {
        UUID failId = UUID.randomUUID();
        UUID okId = UUID.randomUUID();
        OutboxEvent fail = event(failId, "topic", "fail-key", "{}");
        OutboxEvent ok = event(okId, "topic", "ok-key", "{}");

        when(outboxRepository.findPending(50)).thenReturn(List.of(fail, ok));
        doThrow(new RuntimeException("Kafka down"))
                .when(kafkaTemplate).send("topic", "fail-key", "{}");

        processor.process();

        verify(outboxRepository).markFailed(failId);
        verify(outboxRepository, never()).markProcessed(failId);
        verify(outboxRepository).markProcessed(okId);
    }

    @Test
    void process_singleEventKafkaFailure_marksFailedOnly() {
        UUID id = UUID.randomUUID();
        OutboxEvent e = event(id, "topic", "key", "{}");

        when(outboxRepository.findPending(50)).thenReturn(List.of(e));
        doThrow(new RuntimeException("broker unavailable"))
                .when(kafkaTemplate).send("topic", "key", "{}");

        processor.process();

        verify(outboxRepository).markFailed(id);
        verify(outboxRepository, never()).markProcessed(any());
    }

    @Test
    void process_repeatedProcessing_doesNotReprocessAlreadyProcessed() {
        // First batch has one event
        UUID id = UUID.randomUUID();
        OutboxEvent e = event(id, "topic", "key", "{}");
        when(outboxRepository.findPending(50))
                .thenReturn(List.of(e))
                .thenReturn(List.of()); // second poll returns empty

        processor.process();
        processor.process();

        verify(kafkaTemplate, times(1)).send("topic", "key", "{}");
        verify(outboxRepository, times(1)).markProcessed(id);
    }

    private OutboxEvent event(UUID id, String topic, String aggregateId, String payload) {
        OutboxEvent e = new OutboxEvent("TEST", aggregateId, "TEST_EVENT", topic, payload);
        e.setId(id);
        return e;
    }
}
