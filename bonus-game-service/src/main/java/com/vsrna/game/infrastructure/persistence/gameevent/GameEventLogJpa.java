package com.vsrna.game.infrastructure.persistence.gameevent;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"gameEventLog\"", schema = "game")
@Getter
@Setter
@NoArgsConstructor
public class GameEventLogJpa {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "\"roomId\"", nullable = false)
    private UUID roomId;

    @Column(name = "\"eventType\"", nullable = false, length = 64)
    private String eventType;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "\"occurredAt\"", nullable = false)
    private Instant occurredAt;
}
