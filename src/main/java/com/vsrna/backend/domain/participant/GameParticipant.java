package com.vsrna.backend.domain.participant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"gameParticipants\"")
@Getter
@Setter
@NoArgsConstructor
public class GameParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"gameRoomId\"", nullable = false)
    private UUID gameRoomId;

    @Column(name = "\"userId\"")
    private UUID userId;

    @Column(name = "\"isBot\"", nullable = false)
    private boolean isBot;

    @Column(name = "\"reservedPoints\"", nullable = false, precision = 12, scale = 2)
    private BigDecimal reservedPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParticipantStatus status;

    @Column(name = "\"advancedToFinal\"", nullable = false)
    private boolean advancedToFinal;

    @CreationTimestamp
    @Column(name = "\"joinedAt\"", nullable = false, updatable = false)
    private Instant joinedAt;

    public GameParticipant(UUID gameRoomId, UUID userId, boolean isBot, BigDecimal reservedPoints) {
        this.gameRoomId = gameRoomId;
        this.userId = userId;
        this.isBot = isBot;
        this.reservedPoints = reservedPoints;
        this.status = ParticipantStatus.ACTIVE;
        this.advancedToFinal = false;
    }
}
