package com.vsrna.game.infrastructure.persistence.round;

import com.vsrna.game.domain.round.RoundResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoundResultJpaRepository extends JpaRepository<RoundResultJpa, UUID> {

    @Query("""
            SELECT roundResult FROM RoundResultJpa roundResult
            WHERE (:id IS NULL OR roundResult.id = :id)
            AND (:gameRoomId IS NULL OR roundResult.gameRoomId = :gameRoomId)
            AND (:roundNumber IS NULL OR roundResult.roundNumber = :roundNumber)
            AND (:status IS NULL OR roundResult.status = :status)
            ORDER BY roundResult.roundNumber ASC
            """)
    List<RoundResultJpa> findByQuery(
            @Param("id") UUID id,
            @Param("gameRoomId") UUID gameRoomId,
            @Param("roundNumber") Integer roundNumber,
            @Param("status") RoundResultStatus status
    );
}
