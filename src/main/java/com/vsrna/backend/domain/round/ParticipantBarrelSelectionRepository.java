package com.vsrna.backend.domain.round;

import java.util.List;

public interface ParticipantBarrelSelectionRepository {
    List<ParticipantBarrelSelection> createAll(List<ParticipantBarrelSelection> selections);
    List<ParticipantBarrelSelection> list(ParticipantBarrelSelectionQuery query);
    void delete(ParticipantBarrelSelectionQuery query);
}
