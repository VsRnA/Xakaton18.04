package com.prodforge.game.infrastructure.persistence.barrel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BarrelJpaRepository extends JpaRepository<BarrelJpa, UUID> {

    @Query("""
            SELECT barrel FROM BarrelJpa barrel
            WHERE (:id IS NULL OR barrel.id = :id)
            AND (:gameRoomId IS NULL OR barrel.gameRoomId = :gameRoomId)
            AND (:roundNumber IS NULL OR barrel.roundNumber = :roundNumber)
            ORDER BY barrel.displayOrder ASC
            """)
    List<BarrelJpa> findByQuery(
            @Param("id") UUID id,
            @Param("gameRoomId") UUID gameRoomId,
            @Param("roundNumber") Integer roundNumber
    );

    @Modifying
    @Query("UPDATE BarrelJpa barrel SET barrel.weight = :weight WHERE barrel.id = :id")
    int updateWeight(@Param("id") UUID id, @Param("weight") BigDecimal weight);
}
