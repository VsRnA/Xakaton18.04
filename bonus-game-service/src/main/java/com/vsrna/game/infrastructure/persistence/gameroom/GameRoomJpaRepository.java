package com.vsrna.game.infrastructure.persistence.gameroom;

import com.vsrna.game.domain.gameroom.GameRoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRoomJpaRepository extends JpaRepository<GameRoomJpa, UUID> {
    List<GameRoomJpa> findByStatus(GameRoomStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM GameRoomJpa r WHERE r.id = :id")
    Optional<GameRoomJpa> findByIdForUpdate(@Param("id") UUID id);
}
