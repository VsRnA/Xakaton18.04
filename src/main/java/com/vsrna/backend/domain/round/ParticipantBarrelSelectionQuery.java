package com.vsrna.backend.domain.round;

import java.util.UUID;

public record ParticipantBarrelSelectionQuery(UUID entryId, UUID barrelId) {
    public static ParticipantBarrelSelectionQuery byEntry(UUID entryId) {
        return new ParticipantBarrelSelectionQuery(entryId, null);
    }
}
