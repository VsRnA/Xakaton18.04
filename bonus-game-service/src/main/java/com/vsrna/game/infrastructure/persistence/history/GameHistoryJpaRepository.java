package com.vsrna.game.infrastructure.persistence.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GameHistoryJpaRepository extends JpaRepository<GameHistoryJpa, UUID> {

    @Query(value = """
            SELECT * FROM "gameHistory"
            WHERE (CAST(:id AS uuid) IS NULL OR id = CAST(:id AS uuid))
            AND (CAST(:gameRoomId AS uuid) IS NULL OR "gameRoomId" = CAST(:gameRoomId AS uuid))
            AND (CAST(:winnerUserId AS uuid) IS NULL OR "winnerUserId" = CAST(:winnerUserId AS uuid))
            AND (CAST(:from AS timestamptz) IS NULL OR "completedAt" >= CAST(:from AS timestamptz))
            AND (CAST(:to AS timestamptz) IS NULL OR "completedAt" <= CAST(:to AS timestamptz))
            ORDER BY "completedAt" DESC
            """, nativeQuery = true)
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
