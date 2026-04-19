package com.vsrna.game.infrastructure.persistence.participant;

import com.vsrna.game.domain.participant.ParticipantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"gameParticipants\"",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_participant_room_user",
                columnNames = {"\"gameRoomId\"", "\"userId\""}))
@Getter
@Setter
@NoArgsConstructor
public class GameParticipantJpa {

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

    @Column(name = "\"displayName\"", length = 100)
    private String displayName;

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
}
