package com.vsrna.backend.infrastructure.persistence.participant;

import com.vsrna.backend.domain.participant.GameParticipant;
import com.vsrna.backend.domain.participant.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantJpaRepository extends JpaRepository<GameParticipant, UUID> {
    // find: один участник по комнате + пользователь
    Optional<GameParticipant> findByGameRoomIdAndUserId(UUID gameRoomId, UUID userId);

    // list: все участники комнаты
    List<GameParticipant> findByGameRoomId(UUID gameRoomId);

    // list с фильтром по статусу
    List<GameParticipant> findByGameRoomIdAndStatus(UUID gameRoomId, ParticipantStatus status);

    int countByGameRoomId(UUID gameRoomId);
}
