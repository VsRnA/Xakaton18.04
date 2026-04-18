package com.vsrna.backend.infrastructure.persistence.gameroom;

import com.vsrna.backend.domain.gameroom.GameRoomConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameRoomConfigJpaRepository extends JpaRepository<GameRoomConfig, UUID> {
    // find по gameRoomId (gameRoomId — это PK, унаследован findById)
    Optional<GameRoomConfig> findByGameRoomId(UUID gameRoomId);
}
