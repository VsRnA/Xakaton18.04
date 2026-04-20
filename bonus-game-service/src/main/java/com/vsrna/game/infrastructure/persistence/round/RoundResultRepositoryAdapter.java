package com.vsrna.game.infrastructure.persistence.round;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.round.*;
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
        if (query.id() != null) {
            return jpa.findById(query.id()).map(this::toDomain);
        }
        if (query.gameRoomId() != null && query.roundNumber() != null) {
            return jpa.findByGameRoomIdAndRoundNumber(query.gameRoomId(), query.roundNumber())
                    .map(this::toDomain);
        }
        return Optional.empty();
    }

    @Override
    public RoundResult get(RoundResultQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("RoundResult", buildDetail(query)));
    }

    @Override
    public RoundResult update(RoundResultQuery query, RoundResultPatch patch) {
        RoundResultJpa entity = findJpa(query);
        if (patch.status() != null) entity.setStatus(patch.status());
        if (patch.seedHash() != null) entity.setSeedHash(patch.seedHash());
        if (patch.rawSeed() != null) entity.setRawSeed(patch.rawSeed());
        if (patch.endedAt() != null) entity.setEndedAt(patch.endedAt());
        return toDomain(jpa.save(entity));
    }

    private RoundResultJpa findJpa(RoundResultQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id())
                    .orElseThrow(() -> ApiException.notFound("RoundResult", query.id().toString()));
        }
        if (query.gameRoomId() != null && query.roundNumber() != null) {
            return jpa.findByGameRoomIdAndRoundNumber(query.gameRoomId(), query.roundNumber())
                    .orElseThrow(() -> ApiException.notFound("RoundResult", buildDetail(query)));
        }
        throw ApiException.notFound("RoundResult", "unknown");
    }

    private String buildDetail(RoundResultQuery query) {
        if (query.id() != null) return query.id().toString();
        if (query.gameRoomId() != null) return "room=" + query.gameRoomId() + ", round=" + query.roundNumber();
        return "unknown";
    }

    private RoundResult toDomain(RoundResultJpa e) {
        RoundResult rr = new RoundResult();
        rr.setId(e.getId());
        rr.setGameRoomId(e.getGameRoomId());
        rr.setRoundNumber(e.getRoundNumber());
        rr.setStatus(e.getStatus());
        rr.setSeedHash(e.getSeedHash());
        rr.setRawSeed(e.getRawSeed());
        rr.setStartedAt(e.getStartedAt());
        rr.setEndedAt(e.getEndedAt());
        return rr;
    }

    private RoundResultJpa toJpa(RoundResult rr) {
        RoundResultJpa e = new RoundResultJpa();
        e.setId(rr.getId());
        e.setGameRoomId(rr.getGameRoomId());
        e.setRoundNumber(rr.getRoundNumber());
        e.setStatus(rr.getStatus());
        e.setSeedHash(rr.getSeedHash());
        e.setRawSeed(rr.getRawSeed());
        e.setEndedAt(rr.getEndedAt());
        return e;
    }
}
