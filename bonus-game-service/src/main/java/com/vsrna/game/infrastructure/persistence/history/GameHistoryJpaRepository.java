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
            SELECT SUM(h.realPlayersRevenue) FROM GameHistoryJpa h
            """)
    java.math.BigDecimal sumRealPlayersRevenueAll();

    @Query("""
            SELECT SUM(h.prizeAwarded) FROM GameHistoryJpa h
            WHERE h.winnerIsBot = false
            """)
    java.math.BigDecimal sumPrizeAwardedAll();
}
