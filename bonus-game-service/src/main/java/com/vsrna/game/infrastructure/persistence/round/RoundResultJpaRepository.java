package com.vsrna.game.infrastructure.persistence.round;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoundResultJpaRepository extends JpaRepository<RoundResultJpa, UUID> {
    Optional<RoundResultJpa> findByGameRoomIdAndRoundNumber(UUID gameRoomId, int roundNumber);
}
