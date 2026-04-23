package com.prodforge.game.domain.round;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ParticipantRoundEntry {

    private UUID id;
    private UUID roundResultId;
    private UUID participantId;
    private boolean boostPurchased;
    private BigDecimal totalScore;
    private Instant selectionTimestamp;
    private int selectionCount;
    private Integer rankInRound;

    public ParticipantRoundEntry(UUID roundResultId, UUID participantId) {
        this.roundResultId = roundResultId;
        this.participantId = participantId;
        this.boostPurchased = false;
        this.selectionCount = 0;
    }
}
