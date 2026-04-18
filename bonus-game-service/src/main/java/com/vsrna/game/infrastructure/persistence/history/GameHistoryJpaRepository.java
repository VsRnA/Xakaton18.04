package com.vsrna.game.infrastructure.persistence.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameHistoryJpaRepository extends JpaRepository<GameHistoryJpa, UUID> {
    Optional<GameHistoryJpa> findByGameRoomId(UUID gameRoomId);
    List<GameHistoryJpa> findByWinnerUserId(UUID winnerUserId, Pageable pageable);
}
