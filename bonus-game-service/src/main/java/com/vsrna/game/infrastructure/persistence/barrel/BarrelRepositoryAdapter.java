package com.vsrna.game.infrastructure.persistence.barrel;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.barrel.*;
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
        return toDomain(jpa.save(toJpa(barrel)));
    }

    @Override
    public List<Barrel> createAll(List<Barrel> barrels) {
        return jpa.saveAll(barrels.stream().map(this::toJpa).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Barrel> list(BarrelQuery query) {
        if (query.gameRoomId() != null && query.roundNumber() != null) {
            return jpa.findByGameRoomIdAndRoundNumber(query.gameRoomId(), query.roundNumber())
                    .stream().map(this::toDomain).toList();
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId()).stream().map(this::toDomain).toList();
        }
        return List.of();
    }

    @Override
    @Transactional
    public Barrel update(BarrelQuery query, BarrelPatch patch) {
        BarrelJpa entity = jpa.findById(query.id())
                .orElseThrow(() -> ApiException.notFound("Barrel", query.id().toString()));
        if (patch.weight() != null) {
            jpa.updateWeight(entity.getId(), patch.weight());
            entity.setWeight(patch.weight());
        }
        return toDomain(entity);
    }

    @Override
    @Transactional
    public int updateAll(BarrelQuery query, List<Barrel> barrels) {
        int updated = 0;
        for (Barrel barrel : barrels) {
            if (barrel.getWeight() != null) {
                updated += jpa.updateWeight(barrel.getId(), barrel.getWeight());
            }
        }
        return updated;
    }

    private Barrel toDomain(BarrelJpa barrelJpa) {
        Barrel barrel = new Barrel();
        barrel.setId(barrelJpa.getId());
        barrel.setGameRoomId(barrelJpa.getGameRoomId());
        barrel.setRoundNumber(barrelJpa.getRoundNumber());
        barrel.setBarrelCode(barrelJpa.getBarrelCode());
        barrel.setDisplayOrder(barrelJpa.getDisplayOrder());
        barrel.setWeight(barrelJpa.getWeight() != null ? barrelJpa.getWeight().stripTrailingZeros() : null);
        return barrel;
    }

    private BarrelJpa toJpa(Barrel barrel) {
        BarrelJpa jpaEntity = new BarrelJpa();
        jpaEntity.setId(barrel.getId());
        jpaEntity.setGameRoomId(barrel.getGameRoomId());
        jpaEntity.setRoundNumber(barrel.getRoundNumber());
        jpaEntity.setBarrelCode(barrel.getBarrelCode());
        jpaEntity.setDisplayOrder(barrel.getDisplayOrder());
        jpaEntity.setWeight(barrel.getWeight());
        return jpaEntity;
    }
}
