package com.vsrna.backend.infrastructure.persistence.barrel;

import com.vsrna.backend.domain.barrel.Barrel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BarrelJpaRepository extends JpaRepository<Barrel, UUID> {
    // list по комнате и раунду
    List<Barrel> findByGameRoomIdAndRoundNumber(UUID gameRoomId, int roundNumber);

    // list по комнате (оба раунда)
    List<Barrel> findByGameRoomId(UUID gameRoomId);

    // атомарный update веса одной бочки
    @Modifying
    @Query("UPDATE Barrel b SET b.weight = :weight WHERE b.id = :id")
    int updateWeight(@Param("id") UUID id, @Param("weight") BigDecimal weight);
}
