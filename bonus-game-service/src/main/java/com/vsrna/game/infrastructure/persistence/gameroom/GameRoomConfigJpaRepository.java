package com.vsrna.game.infrastructure.persistence.gameroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRoomConfigJpaRepository extends JpaRepository<GameRoomConfigJpa, UUID> {
    Optional<GameRoomConfigJpa> findByGameRoomId(UUID gameRoomId);

    @Query("SELECT c FROM GameRoomConfigJpa c WHERE c.gameRoomId IN :roomIds")
    List<GameRoomConfigJpa> findByGameRoomIdIn(@Param("roomIds") List<UUID> roomIds);
}
