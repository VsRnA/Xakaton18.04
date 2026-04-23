package com.prodforge.game.infrastructure.persistence.round;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ParticipantRoundEntryJpaRepository extends JpaRepository<ParticipantRoundEntryJpa, UUID> {

    @Query("""
            SELECT entry FROM ParticipantRoundEntryJpa entry
            WHERE (:id IS NULL OR entry.id = :id)
            AND (:roundResultId IS NULL OR entry.roundResultId = :roundResultId)
            AND (:participantId IS NULL OR entry.participantId = :participantId)
            AND (:rankInRound IS NULL OR entry.rankInRound = :rankInRound)
            """)
    List<ParticipantRoundEntryJpa> findByQuery(
            @Param("id") UUID id,
            @Param("roundResultId") UUID roundResultId,
            @Param("participantId") UUID participantId,
            @Param("rankInRound") Integer rankInRound
    );

    @Query("SELECT COUNT(entry) FROM ParticipantRoundEntryJpa entry WHERE entry.roundResultId = :roundResultId")
    int countByRoundResultId(@Param("roundResultId") UUID roundResultId);
}
