package com.vsrna.backend.infrastructure.persistence.round;

import com.vsrna.backend.domain.round.RoundResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoundResultJpaRepository extends JpaRepository<RoundResult, UUID> {
    Optional<RoundResult> findByGameRoomIdAndRoundNumber(UUID gameRoomId, int roundNumber);
}
