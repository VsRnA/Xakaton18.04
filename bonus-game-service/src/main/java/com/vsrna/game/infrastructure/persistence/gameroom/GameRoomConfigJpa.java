package com.vsrna.game.infrastructure.persistence.gameroom;

import com.vsrna.game.domain.gameroom.RepeatInterval;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"gameRoomConfig\"")
@Getter
@Setter
@NoArgsConstructor
public class GameRoomConfigJpa {

    @Id
    @Column(name = "\"gameRoomId\"", nullable = false)
    private UUID gameRoomId;

    @Column(name = "\"maxPlayers\"", nullable = false)
    private int maxPlayers;

    @Column(name = "\"entryFeeAmount\"", nullable = false, precision = 12, scale = 2)
    private BigDecimal entryFeeAmount;

    @Column(name = "\"winnerPayoutPercentage\"", nullable = false, precision = 5, scale = 2)
    private BigDecimal winnerPayoutPercentage;

    @Column(name = "\"boostCostAmount\"", nullable = false, precision = 12, scale = 2)
    private BigDecimal boostCostAmount;

    @Column(name = "\"isBoostEnabled\"", nullable = false)
    private boolean isBoostEnabled;

    @Column(name = "\"maxBarrelSelection\"", nullable = false)
    private int maxBarrelSelection;

    @Column(name = "\"scheduledStartAt\"", nullable = true)
    private Instant scheduledStartAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "\"repeatInterval\"", nullable = true, length = 20)
    private RepeatInterval repeatInterval;
}
