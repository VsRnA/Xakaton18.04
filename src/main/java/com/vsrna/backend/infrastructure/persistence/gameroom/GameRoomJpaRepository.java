package com.vsrna.backend.infrastructure.persistence.gameroom;

import com.vsrna.backend.domain.gameroom.GameRoom;
import com.vsrna.backend.domain.gameroom.GameRoomStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameRoomJpaRepository extends JpaRepository<GameRoom, UUID> {
    // list с фильтром по статусу
    List<GameRoom> findByStatus(GameRoomStatus status, Pageable pageable);
    // list без фильтра (findAll(Pageable) унаследован от JpaRepository)
}
