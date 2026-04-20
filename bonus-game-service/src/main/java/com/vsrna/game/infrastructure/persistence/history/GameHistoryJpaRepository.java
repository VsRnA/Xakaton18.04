package com.vsrna.game.infrastructure.persistence.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameHistoryJpaRepository extends JpaRepository<GameHistoryJpa, UUID> {
    Optional<GameHistoryJpa> findByGameRoomId(UUID gameRoomId);
    List<GameHistoryJpa> findByWinnerUserId(UUID winnerUserId, Pageable pageable);
    List<GameHistoryJpa> findByCompletedAtBetween(Instant from, Instant to);

    @Query("""
            SELECT COALESCE(SUM(h.realPlayersRevenue), 0)
                 - COALESCE(SUM(CASE WHEN h.winnerIsBot = false THEN h.prizeAwarded ELSE 0 END), 0)
            FROM GameHistoryJpa h
            """)
    java.math.BigDecimal getSystemBalance();
}
