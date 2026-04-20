package com.vsrna.game.infrastructure.persistence.round;

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
public class ParticipantBarrelSelectionJpa {

    @EmbeddedId
    private ParticipantBarrelSelectionIdJpa id;

    public ParticipantBarrelSelectionJpa(UUID entryId, UUID barrelId) {
        this.id = new ParticipantBarrelSelectionIdJpa(entryId, barrelId);
    }

    public UUID getEntryId() {
        return id.getEntryId();
    }

    public UUID getBarrelId() {
        return id.getBarrelId();
    }
}
