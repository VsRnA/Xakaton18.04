package com.prodforge.game.domain.round;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParticipantBarrelSelectionId {

    private UUID entryId;
    private UUID barrelId;
}
