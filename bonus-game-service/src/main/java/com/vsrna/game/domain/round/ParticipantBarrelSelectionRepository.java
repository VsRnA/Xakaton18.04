package com.vsrna.game.domain.round;

import java.util.List;
import java.util.UUID;

public interface ParticipantBarrelSelectionRepository {
    List<ParticipantBarrelSelection> createAll(List<ParticipantBarrelSelection> selections);
    List<ParticipantBarrelSelection> list(ParticipantBarrelSelectionQuery query);
    List<ParticipantBarrelSelection> listByEntries(List<UUID> entryIds);
    void delete(ParticipantBarrelSelectionQuery query);
}
