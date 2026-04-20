package com.vsrna.game.domain.round;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RoundResult {

    private UUID id;
    private UUID gameRoomId;
    private int roundNumber;
    private RoundResultStatus status;
    private String seedHash;
    private String rawSeed;
    private Instant startedAt;
    private Instant endedAt;

    public RoundResult(UUID gameRoomId, int roundNumber) {
        this.gameRoomId = gameRoomId;
        this.roundNumber = roundNumber;
        this.status = RoundResultStatus.IN_PROGRESS;
    }
}
