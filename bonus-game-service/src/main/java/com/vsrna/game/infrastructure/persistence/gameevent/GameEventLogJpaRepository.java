package com.vsrna.game.infrastructure.persistence.gameevent;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameEventLogJpaRepository extends JpaRepository<GameEventLogJpa, UUID> {
    List<GameEventLogJpa> findByRoomIdOrderByOccurredAtAsc(UUID roomId, Pageable pageable);
}
