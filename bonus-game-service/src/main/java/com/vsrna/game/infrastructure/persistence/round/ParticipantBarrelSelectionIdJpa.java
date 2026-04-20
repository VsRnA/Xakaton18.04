package com.vsrna.game.infrastructure.persistence.round;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParticipantBarrelSelectionIdJpa implements Serializable {

    @Column(name = "\"entryId\"", nullable = false)
    private UUID entryId;

    @Column(name = "\"barrelId\"", nullable = false)
    private UUID barrelId;
}
