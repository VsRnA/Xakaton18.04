package com.vsrna.game.domain.round;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ParticipantBarrelSelection {

    private UUID entryId;
    private UUID barrelId;

    public ParticipantBarrelSelection(UUID entryId, UUID barrelId) {
        this.entryId = entryId;
        this.barrelId = barrelId;
    }
}
