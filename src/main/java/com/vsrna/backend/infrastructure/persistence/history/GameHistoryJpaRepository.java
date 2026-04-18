package com.vsrna.backend.infrastructure.persistence.history;

import com.vsrna.backend.domain.history.GameHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameHistoryJpaRepository extends JpaRepository<GameHistory, UUID> {
    Optional<GameHistory> findByGameRoomId(UUID gameRoomId);
    List<GameHistory> findByWinnerUserId(UUID winnerUserId, Pageable pageable);
}
