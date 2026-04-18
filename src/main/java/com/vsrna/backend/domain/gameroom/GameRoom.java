package com.vsrna.backend.domain.gameroom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"gameRooms\"")
@Getter
@Setter
@NoArgsConstructor
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameRoomStatus status;

    @Column(name = "\"createdByUserId\"", nullable = false)
    private UUID createdByUserId;

    @CreationTimestamp
    @Column(name = "\"createdAt\"", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "\"startedAt\"")
    private Instant startedAt;

    @Column(name = "\"finishedAt\"")
    private Instant finishedAt;

    @Column(name = "\"currentPlayerCount\"", nullable = false)
    private int currentPlayerCount;

    @Column(name = "\"prizePoolAmount\"", nullable = false, precision = 12, scale = 2)
    private BigDecimal prizePoolAmount;

    public GameRoom(UUID createdByUserId, BigDecimal prizePoolAmount) {
        this.createdByUserId = createdByUserId;
        this.status = GameRoomStatus.WAITING;
        this.currentPlayerCount = 0;
        this.prizePoolAmount = prizePoolAmount;
    }
}
