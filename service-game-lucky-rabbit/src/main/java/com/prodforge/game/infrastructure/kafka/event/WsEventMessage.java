package com.prodforge.game.infrastructure.kafka.event;

import java.util.Map;

public record WsEventMessage(
        String destination,
        String userId,
        Map<String, Object> payload
) {}
