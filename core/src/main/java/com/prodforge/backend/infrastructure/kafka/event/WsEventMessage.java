package com.prodforge.backend.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record WsEventMessage(
        @JsonProperty("destination") String destination,
        @JsonProperty("userId") String userId,
        @JsonProperty("payload") Map<String, Object> payload
) {}
