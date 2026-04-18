package com.vsrna.backend.domain.barrel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "barrels")
@Getter
@Setter
@NoArgsConstructor
public class Barrel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "\"gameRoomId\"", nullable = false)
    private UUID gameRoomId;

    @Column(name = "\"roundNumber\"", nullable = false)
    private int roundNumber;

    @Column(name = "\"barrelCode\"", nullable = false)
    private String barrelCode;

    @Column(name = "\"displayOrder\"", nullable = false)
    private int displayOrder;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    public Barrel(UUID gameRoomId, int roundNumber, String barrelCode, int displayOrder) {
        this.gameRoomId = gameRoomId;
        this.roundNumber = roundNumber;
        this.barrelCode = barrelCode;
        this.displayOrder = displayOrder;
        this.weight = null;
    }
}
