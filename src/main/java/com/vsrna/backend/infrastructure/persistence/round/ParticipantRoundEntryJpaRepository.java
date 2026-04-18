package com.vsrna.backend.infrastructure.persistence.round;

import com.vsrna.backend.domain.round.ParticipantRoundEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRoundEntryJpaRepository extends JpaRepository<ParticipantRoundEntry, UUID> {
    Optional<ParticipantRoundEntry> findByRoundResultIdAndParticipantId(UUID roundResultId, UUID participantId);
    List<ParticipantRoundEntry> findByRoundResultId(UUID roundResultId);
}
