package com.vsrna.game.infrastructure.persistence.barrel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BarrelJpaRepository extends JpaRepository<BarrelJpa, UUID> {
    List<BarrelJpa> findByGameRoomIdAndRoundNumber(UUID gameRoomId, int roundNumber);
    List<BarrelJpa> findByGameRoomId(UUID gameRoomId);

    @Modifying
    @Query("UPDATE BarrelJpa b SET b.weight = :weight WHERE b.id = :id")
    int updateWeight(@Param("id") UUID id, @Param("weight") BigDecimal weight);
}
