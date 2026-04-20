package com.vsrna.game.infrastructure.persistence.round;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ParticipantBarrelSelectionJpaRepository
        extends JpaRepository<ParticipantBarrelSelectionJpa, ParticipantBarrelSelectionIdJpa> {

    List<ParticipantBarrelSelectionJpa> findByIdEntryId(UUID entryId);

    @Query("SELECT s FROM ParticipantBarrelSelectionJpa s WHERE s.id.entryId IN :entryIds")
    List<ParticipantBarrelSelectionJpa> findByEntryIds(@Param("entryIds") List<UUID> entryIds);

    @Modifying
    void deleteByIdEntryId(UUID entryId);
}
