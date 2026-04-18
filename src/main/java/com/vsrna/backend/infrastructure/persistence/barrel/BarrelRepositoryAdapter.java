package com.vsrna.backend.infrastructure.persistence.barrel;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.barrel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BarrelRepositoryAdapter implements BarrelRepository {

    private final BarrelJpaRepository jpa;

    @Override
    public Barrel create(Barrel barrel) {
        return jpa.save(barrel);
    }

    @Override
    public List<Barrel> createAll(List<Barrel> barrels) {
        return jpa.saveAll(barrels);
    }

    @Override
    public List<Barrel> list(BarrelQuery query) {
        if (query.gameRoomId() != null && query.roundNumber() != null) {
            return jpa.findByGameRoomIdAndRoundNumber(query.gameRoomId(), query.roundNumber());
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId());
        }
        return List.of();
    }

    @Override
    @Transactional
    public Barrel update(BarrelQuery query, BarrelPatch patch) {
        Barrel barrel = jpa.findById(query.id())
                .orElseThrow(() -> ApiException.notFound("Barrel", query.id().toString()));
        if (patch.weight() != null) {
            jpa.updateWeight(barrel.getId(), patch.weight());
            barrel.setWeight(patch.weight());
        }
        return barrel;
    }

    @Override
    @Transactional
    public int updateAll(BarrelQuery query, List<Barrel> barrels) {
        int updated = 0;
        for (Barrel b : barrels) {
            if (b.getWeight() != null) {
                updated += jpa.updateWeight(b.getId(), b.getWeight());
            }
        }
        return updated;
    }
}
