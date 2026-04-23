package com.prodforge.game.domain.gameevent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GameEventLog {

    private UUID id;
    private UUID roomId;
    private String eventType;
    private Map<String, Object> details;
    private Instant occurredAt;

    public GameEventLog(UUID roomId, String eventType, Map<String, Object> details) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.eventType = eventType;
        this.details = details;
        this.occurredAt = Instant.now();
    }
}
