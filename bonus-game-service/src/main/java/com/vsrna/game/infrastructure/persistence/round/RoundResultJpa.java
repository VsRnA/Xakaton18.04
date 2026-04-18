package com.vsrna.game.infrastructure.persistence.round;

import com.vsrna.game.domain.round.RoundResultStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"roundResults\"")
@Getter
@Setter
@NoArgsConstructor
public class RoundResultJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"gameRoomId\"", nullable = false)
    private UUID gameRoomId;

    @Column(name = "\"roundNumber\"", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoundResultStatus status;

    @Column(name = "\"seedHash\"")
    private String seedHash;

    @Column(name = "\"rawSeed\"")
    private String rawSeed;

    @CreationTimestamp
    @Column(name = "\"startedAt\"", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "\"endedAt\"")
    private Instant endedAt;
}
