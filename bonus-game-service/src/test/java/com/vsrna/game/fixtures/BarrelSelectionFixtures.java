package com.vsrna.game.fixtures;

import com.vsrna.game.domain.round.ParticipantBarrelSelection;

import java.util.UUID;

public final class BarrelSelectionFixtures {

    private BarrelSelectionFixtures() {}

    public static ParticipantBarrelSelection selection(UUID entryId, UUID barrelId) {
        return new ParticipantBarrelSelection(entryId, barrelId);
    }
}
