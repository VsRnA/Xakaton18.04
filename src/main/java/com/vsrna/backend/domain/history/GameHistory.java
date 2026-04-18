package com.vsrna.backend.domain.history;

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
public class GameHistory {

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

    public GameHistory(UUID gameRoomId, UUID winnerUserId, boolean winnerIsBot,
                       BigDecimal prizeAwarded, BigDecimal systemRevenue, String winCriteria) {
        this.gameRoomId = gameRoomId;
        this.winnerUserId = winnerUserId;
        this.winnerIsBot = winnerIsBot;
        this.prizeAwarded = prizeAwarded;
        this.systemRevenue = systemRevenue;
        this.winCriteria = winCriteria;
    }
}
