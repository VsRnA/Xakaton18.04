package com.vsrna.game.infrastructure.persistence.round;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"participantRoundEntries\"")
@Getter
@Setter
@NoArgsConstructor
public class ParticipantRoundEntryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"roundResultId\"", nullable = false)
    private UUID roundResultId;

    @Column(name = "\"participantId\"", nullable = false)
    private UUID participantId;

    @Column(name = "\"boostPurchased\"", nullable = false)
    private boolean boostPurchased;

    @Column(name = "\"discardedBarrelId\"")
    private UUID boostedBarrelId;

    @Column(name = "\"totalScore\"", precision = 8, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "\"selectionTimestamp\"")
    private Instant selectionTimestamp;

    @Column(name = "\"selectionCount\"", nullable = false)
    private int selectionCount;

    @Column(name = "\"rankInRound\"")
    private Integer rankInRound;
}
