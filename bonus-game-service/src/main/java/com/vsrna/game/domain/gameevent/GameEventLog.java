package com.vsrna.game.domain.gameevent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GameEventLog {

    private UUID id;
    private UUID roomId;
    private String eventType;
    private String details;
    private Instant occurredAt;

    public GameEventLog(UUID roomId, String eventType, String details) {
        this.id = UUID.randomUUID();
        this.roomId = roomId;
        this.eventType = eventType;
        this.details = details;
        this.occurredAt = Instant.now();
    }
}
