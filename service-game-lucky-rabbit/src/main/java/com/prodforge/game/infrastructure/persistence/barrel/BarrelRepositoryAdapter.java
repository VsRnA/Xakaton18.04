package com.prodforge.game.infrastructure.persistence.barrel;

import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.barrel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return jpa.findByQuery(query.id(), query.gameRoomId(), query.roundNumber())
                .stream().map(this::toDomain).toList();
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
        List<Barrel> barrelsWithWeight = barrels.stream()
                .filter(barrel -> barrel.getWeight() != null)
                .toList();
        if (barrelsWithWeight.isEmpty()) {
            return 0;
        }
        List<UUID> ids = barrelsWithWeight.stream().map(Barrel::getId).toList();
        Map<UUID, BigDecimal> weightById = barrelsWithWeight.stream()
                .collect(Collectors.toMap(Barrel::getId, Barrel::getWeight));
        List<BarrelJpa> jpaEntities = jpa.findAllById(ids);
        jpaEntities.forEach(entity -> entity.setWeight(weightById.get(entity.getId())));
        jpa.saveAll(jpaEntities);
        return jpaEntities.size();
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
