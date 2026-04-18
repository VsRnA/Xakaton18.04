package com.vsrna.backend.infrastructure.persistence.round;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ParticipantRoundEntryRepositoryAdapter implements ParticipantRoundEntryRepository {

    private final ParticipantRoundEntryJpaRepository jpa;

    @Override
    public ParticipantRoundEntry create(ParticipantRoundEntry entry) {
        return jpa.save(entry);
    }

    @Override
    public Optional<ParticipantRoundEntry> find(ParticipantRoundEntryQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id());
        }
        if (query.roundResultId() != null && query.participantId() != null) {
            return jpa.findByRoundResultIdAndParticipantId(query.roundResultId(), query.participantId());
        }
        return Optional.empty();
    }

    @Override
    public ParticipantRoundEntry get(ParticipantRoundEntryQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("ParticipantRoundEntry", buildDetail(query)));
    }

    @Override
    public List<ParticipantRoundEntry> list(ParticipantRoundEntryQuery query) {
        if (query.roundResultId() != null) {
            return jpa.findByRoundResultId(query.roundResultId());
        }
        return List.of();
    }

    @Override
    public ParticipantRoundEntry update(ParticipantRoundEntryQuery query, ParticipantRoundEntryPatch patch) {
        ParticipantRoundEntry entry = get(query);
        if (patch.boostPurchased() != null) entry.setBoostPurchased(patch.boostPurchased());
        if (patch.discardedBarrelId() != null) entry.setDiscardedBarrelId(patch.discardedBarrelId());
        if (patch.totalScore() != null) entry.setTotalScore(patch.totalScore());
        if (patch.selectionTimestamp() != null) entry.setSelectionTimestamp(patch.selectionTimestamp());
        if (patch.selectionCount() != null) entry.setSelectionCount(patch.selectionCount());
        if (patch.rankInRound() != null) entry.setRankInRound(patch.rankInRound());
        return jpa.save(entry);
    }

    private String buildDetail(ParticipantRoundEntryQuery query) {
        if (query.id() != null) return query.id().toString();
        return "roundResult=" + query.roundResultId() + ", participant=" + query.participantId();
    }
}
