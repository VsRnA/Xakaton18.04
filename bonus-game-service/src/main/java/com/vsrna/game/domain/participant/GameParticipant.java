package com.vsrna.game.domain.participant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GameParticipant {

    private UUID id;
    private UUID gameRoomId;
    private UUID userId;
    private boolean isBot;
    private String displayName;
    private BigDecimal reservedPoints;
    private ParticipantStatus status;
    private boolean advancedToFinal;
    private Instant joinedAt;

    public GameParticipant(UUID gameRoomId, UUID userId, boolean isBot, String displayName, BigDecimal reservedPoints) {
        this.gameRoomId = gameRoomId;
        this.userId = userId;
        this.isBot = isBot;
        this.displayName = displayName;
        this.reservedPoints = reservedPoints;
        this.status = ParticipantStatus.ACTIVE;
        this.advancedToFinal = false;
    }
}
