package com.vsrna.game.infrastructure.persistence.participant;

import com.vsrna.game.domain.participant.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameParticipantJpaRepository extends JpaRepository<GameParticipantJpa, UUID> {

    @Query("""
            SELECT participant FROM GameParticipantJpa participant
            WHERE (:id IS NULL OR participant.id = :id)
            AND (:gameRoomId IS NULL OR participant.gameRoomId = :gameRoomId)
            AND (:userId IS NULL OR participant.userId = :userId)
            AND (:status IS NULL OR participant.status = :status)
            ORDER BY participant.joinedAt ASC
            """)
    List<GameParticipantJpa> findByQuery(
            @Param("id") UUID id,
            @Param("gameRoomId") UUID gameRoomId,
            @Param("userId") UUID userId,
            @Param("status") ParticipantStatus status
    );

    @Query("SELECT COUNT(participant) FROM GameParticipantJpa participant WHERE participant.gameRoomId = :gameRoomId")
    int countByGameRoomId(@Param("gameRoomId") UUID gameRoomId);
}
