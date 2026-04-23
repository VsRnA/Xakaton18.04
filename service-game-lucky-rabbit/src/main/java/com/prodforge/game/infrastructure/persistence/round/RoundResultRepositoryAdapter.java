package com.prodforge.game.infrastructure.persistence.round;

import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoundResultRepositoryAdapter implements RoundResultRepository {

    private final RoundResultJpaRepository jpa;

    @Override
    public RoundResult create(RoundResult roundResult) {
        return toDomain(jpa.save(toJpa(roundResult)));
    }

    @Override
    public Optional<RoundResult> find(RoundResultQuery query) {
        return jpa.findByQuery(query.id(), query.gameRoomId(), query.roundNumber(), query.status())
                .stream().findFirst().map(this::toDomain);
    }

    @Override
    public RoundResult get(RoundResultQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("RoundResult", buildDetail(query)));
    }

    @Override
    public RoundResult update(RoundResultQuery query, RoundResultPatch patch) {
        RoundResultJpa entity = findJpa(query);
        applyPatch(entity, patch);
        return toDomain(jpa.save(entity));
    }

    private void applyPatch(RoundResultJpa entity, RoundResultPatch patch) {
        if (patch.status() != null) entity.setStatus(patch.status());
        if (patch.seedHash() != null) entity.setSeedHash(patch.seedHash());
        if (patch.rawSeed() != null) entity.setRawSeed(patch.rawSeed());
        if (patch.endedAt() != null) entity.setEndedAt(patch.endedAt());
    }

    private RoundResultJpa findJpa(RoundResultQuery query) {
        return jpa.findByQuery(query.id(), query.gameRoomId(), query.roundNumber(), query.status())
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("RoundResult", buildDetail(query)));
    }

    private String buildDetail(RoundResultQuery query) {
        if (query.id() != null) return query.id().toString();
        if (query.gameRoomId() != null) return "room=" + query.gameRoomId() + ", round=" + query.roundNumber();
        return "unknown";
    }

    private RoundResult toDomain(RoundResultJpa jpaEntity) {
        RoundResult roundResult = new RoundResult();
        roundResult.setId(jpaEntity.getId());
        roundResult.setGameRoomId(jpaEntity.getGameRoomId());
        roundResult.setRoundNumber(jpaEntity.getRoundNumber());
        roundResult.setStatus(jpaEntity.getStatus());
        roundResult.setSeedHash(jpaEntity.getSeedHash());
        roundResult.setRawSeed(jpaEntity.getRawSeed());
        roundResult.setStartedAt(jpaEntity.getStartedAt());
        roundResult.setEndedAt(jpaEntity.getEndedAt());
        return roundResult;
    }

    private RoundResultJpa toJpa(RoundResult roundResult) {
        RoundResultJpa jpaEntity = new RoundResultJpa();
        jpaEntity.setId(roundResult.getId());
        jpaEntity.setGameRoomId(roundResult.getGameRoomId());
        jpaEntity.setRoundNumber(roundResult.getRoundNumber());
        jpaEntity.setStatus(roundResult.getStatus());
        jpaEntity.setSeedHash(roundResult.getSeedHash());
        jpaEntity.setRawSeed(roundResult.getRawSeed());
        jpaEntity.setEndedAt(roundResult.getEndedAt());
        return jpaEntity;
    }
}
