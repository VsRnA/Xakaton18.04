package com.vsrna.game.infrastructure.persistence.participant;

import com.vsrna.game.domain.participant.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantJpaRepository extends JpaRepository<GameParticipantJpa, UUID> {
    Optional<GameParticipantJpa> findByGameRoomIdAndUserId(UUID gameRoomId, UUID userId);
    List<GameParticipantJpa> findByGameRoomId(UUID gameRoomId);
    List<GameParticipantJpa> findByGameRoomIdAndStatus(UUID gameRoomId, ParticipantStatus status);
    int countByGameRoomId(UUID gameRoomId);
}
