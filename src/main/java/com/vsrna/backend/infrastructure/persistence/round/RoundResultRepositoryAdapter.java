package com.vsrna.backend.infrastructure.persistence.round;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoundResultRepositoryAdapter implements RoundResultRepository {

    private final RoundResultJpaRepository jpa;

    @Override
    public RoundResult create(RoundResult roundResult) {
        return jpa.save(roundResult);
    }

    @Override
    public Optional<RoundResult> find(RoundResultQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id());
        }
        if (query.gameRoomId() != null && query.roundNumber() != null) {
            return jpa.findByGameRoomIdAndRoundNumber(query.gameRoomId(), query.roundNumber());
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
        RoundResult rr = get(query);
        if (patch.status() != null) rr.setStatus(patch.status());
        if (patch.seedHash() != null) rr.setSeedHash(patch.seedHash());
        if (patch.rawSeed() != null) rr.setRawSeed(patch.rawSeed());
        if (patch.endedAt() != null) rr.setEndedAt(patch.endedAt());
        return jpa.save(rr);
    }

    private String buildDetail(RoundResultQuery query) {
        if (query.id() != null) return query.id().toString();
        if (query.gameRoomId() != null) return "room=" + query.gameRoomId() + ", round=" + query.roundNumber();
        return "unknown";
    }
}
