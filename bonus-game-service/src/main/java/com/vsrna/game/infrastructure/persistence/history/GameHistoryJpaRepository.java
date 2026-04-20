package com.vsrna.game.infrastructure.persistence.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GameHistoryJpaRepository extends JpaRepository<GameHistoryJpa, UUID> {

    @Query("""
            SELECT history FROM GameHistoryJpa history
            WHERE (:id IS NULL OR history.id = :id)
            AND (:gameRoomId IS NULL OR history.gameRoomId = :gameRoomId)
            AND (:winnerUserId IS NULL OR history.winnerUserId = :winnerUserId)
            AND (:from IS NULL OR history.completedAt >= :from)
            AND (:to IS NULL OR history.completedAt <= :to)
            ORDER BY history.completedAt DESC
            """)
    List<GameHistoryJpa> findByQuery(
            @Param("id") UUID id,
            @Param("gameRoomId") UUID gameRoomId,
            @Param("winnerUserId") UUID winnerUserId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(history.realPlayersRevenue), 0)
                 - COALESCE(SUM(CASE WHEN history.winnerIsBot = false THEN history.prizeAwarded ELSE 0 END), 0)
            FROM GameHistoryJpa history
            """)
    java.math.BigDecimal getSystemBalance();
}
