package com.vsrna.backend.infrastructure.kafka;

import com.vsrna.backend.infrastructure.kafka.event.WsEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsEventRelay {

    private final SimpMessagingTemplate messaging;

    @KafkaListener(topics = "game.ws.event", groupId = "stoloto-core",
            containerFactory = "wsEventContainerFactory")
    public void handleWsEvent(WsEventMessage event) {
        log.debug("Relaying WS event to {}, userId={}", event.destination(), event.userId());
        if (event.userId() != null) {
            messaging.convertAndSendToUser(event.userId(), event.destination(), event.payload());
        } else {
            messaging.convertAndSend(event.destination(), event.payload());
        }
    }
}
