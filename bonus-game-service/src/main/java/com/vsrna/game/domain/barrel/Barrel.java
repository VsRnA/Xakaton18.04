package com.vsrna.game.domain.barrel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Barrel {

    private UUID id;
    private UUID gameRoomId;
    private int roundNumber;
    private String barrelCode;
    private int displayOrder;
    private BigDecimal weight;

    public Barrel(UUID gameRoomId, int roundNumber, String barrelCode, int displayOrder) {
        this.gameRoomId = gameRoomId;
        this.roundNumber = roundNumber;
        this.barrelCode = barrelCode;
        this.displayOrder = displayOrder;
        this.weight = null;
    }
}
