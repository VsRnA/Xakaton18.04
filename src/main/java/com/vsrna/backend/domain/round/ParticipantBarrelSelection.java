package com.vsrna.backend.domain.round;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "\"participantBarrelSelections\"")
@Getter
@Setter
@NoArgsConstructor
public class ParticipantBarrelSelection {

    @EmbeddedId
    private ParticipantBarrelSelectionId id;

    public ParticipantBarrelSelection(UUID entryId, UUID barrelId) {
        this.id = new ParticipantBarrelSelectionId(entryId, barrelId);
    }

    public UUID getEntryId() {
        return id.getEntryId();
    }

    public UUID getBarrelId() {
        return id.getBarrelId();
    }
}
