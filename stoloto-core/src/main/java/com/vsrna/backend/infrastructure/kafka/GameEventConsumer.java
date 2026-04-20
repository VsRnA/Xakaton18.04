package com.vsrna.backend.infrastructure.kafka;

import com.vsrna.backend.infrastructure.kafka.event.GameEntryReservedEvent;
import com.vsrna.backend.infrastructure.kafka.event.GameFinishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameEventConsumer {

    @KafkaListener(topics = "game.finished", groupId = "stoloto-core",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleGameFinished(GameFinishedEvent event) {
        log.info("Game finished: roomId={}, winnerId={}, prizeAwarded={}, systemRevenue={}",
                event.roomId(), event.winnerId(), event.prizeAwarded(), event.systemRevenue());
    }

    @KafkaListener(topics = "game.entry.reserved", groupId = "stoloto-core",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleEntryReserved(GameEntryReservedEvent event) {
        log.info("Entry reserved: userId={}, roomId={}, amount={}",
                event.userId(), event.roomId(), event.amount());
    }
}
