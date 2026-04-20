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
        for (Barrel b : barrels) {
            if (b.getWeight() != null) {
                updated += jpa.updateWeight(b.getId(), b.getWeight());
            }
        }
        return updated;
    }

    private Barrel toDomain(BarrelJpa e) {
        Barrel b = new Barrel();
        b.setId(e.getId());
        b.setGameRoomId(e.getGameRoomId());
        b.setRoundNumber(e.getRoundNumber());
        b.setBarrelCode(e.getBarrelCode());
        b.setDisplayOrder(e.getDisplayOrder());
        b.setWeight(e.getWeight() != null ? e.getWeight().stripTrailingZeros() : null);
        return b;
    }

    private BarrelJpa toJpa(Barrel b) {
        BarrelJpa e = new BarrelJpa();
        e.setId(b.getId());
        e.setGameRoomId(b.getGameRoomId());
        e.setRoundNumber(b.getRoundNumber());
        e.setBarrelCode(b.getBarrelCode());
        e.setDisplayOrder(b.getDisplayOrder());
        e.setWeight(b.getWeight());
        return e;
    }
}
