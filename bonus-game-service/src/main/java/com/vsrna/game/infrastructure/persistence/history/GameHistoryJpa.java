package com.vsrna.game.infrastructure.persistence.history;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"gameHistory\"")
@Getter
@Setter
@NoArgsConstructor
public class GameHistoryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"gameRoomId\"", nullable = false, unique = true)
    private UUID gameRoomId;

    @Column(name = "\"winnerUserId\"")
    private UUID winnerUserId;

    @Column(name = "\"winnerIsBot\"", nullable = false)
    private boolean winnerIsBot;

    @Column(name = "\"prizeAwarded\"", precision = 12, scale = 2)
    private BigDecimal prizeAwarded;

    @Column(name = "\"systemRevenue\"", precision = 12, scale = 2)
    private BigDecimal systemRevenue;

    @CreationTimestamp
    @Column(name = "\"completedAt\"", nullable = false, updatable = false)
    private Instant completedAt;

    @Column(name = "\"winCriteria\"")
    private String winCriteria;

    @Column(name = "\"summaryJson\"", columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "\"realPlayersCount\"", nullable = false)
    private int realPlayersCount;

    @Column(name = "\"botCount\"", nullable = false)
    private int botCount;

    @Column(name = "\"realPlayersRevenue\"", precision = 12, scale = 2)
    private BigDecimal realPlayersRevenue;

    @Column(name = "\"boostRevenue\"", precision = 12, scale = 2)
    private BigDecimal boostRevenue;

    @Column(name = "\"boostUsedCount\"", nullable = false)
    private int boostUsedCount;

    @Column(name = "\"winnerUsedBoost\"", nullable = false)
    private boolean winnerUsedBoost;
}
