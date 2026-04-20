package com.vsrna.game.infrastructure.persistence.round;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRoundEntryJpaRepository extends JpaRepository<ParticipantRoundEntryJpa, UUID> {
    Optional<ParticipantRoundEntryJpa> findByRoundResultIdAndParticipantId(UUID roundResultId, UUID participantId);
    List<ParticipantRoundEntryJpa> findByRoundResultId(UUID roundResultId);
    int countByRoundResultId(UUID roundResultId);
}
