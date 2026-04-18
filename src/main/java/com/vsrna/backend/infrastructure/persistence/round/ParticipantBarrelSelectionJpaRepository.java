package com.vsrna.backend.infrastructure.persistence.round;

import com.vsrna.backend.domain.round.ParticipantBarrelSelection;
import com.vsrna.backend.domain.round.ParticipantBarrelSelectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface ParticipantBarrelSelectionJpaRepository
        extends JpaRepository<ParticipantBarrelSelection, ParticipantBarrelSelectionId> {

    List<ParticipantBarrelSelection> findByIdEntryId(UUID entryId);

    @Modifying
    void deleteByIdEntryId(UUID entryId);
}
