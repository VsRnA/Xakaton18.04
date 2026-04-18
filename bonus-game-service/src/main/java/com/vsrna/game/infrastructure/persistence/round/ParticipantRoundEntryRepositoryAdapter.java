package com.vsrna.game.infrastructure.persistence.round;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ParticipantRoundEntryRepositoryAdapter implements ParticipantRoundEntryRepository {

    private final ParticipantRoundEntryJpaRepository jpa;

    @Override
    public ParticipantRoundEntry create(ParticipantRoundEntry entry) {
        return toDomain(jpa.save(toJpa(entry)));
    }

    @Override
    public Optional<ParticipantRoundEntry> find(ParticipantRoundEntryQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id()).map(this::toDomain);
        }
        if (query.roundResultId() != null && query.participantId() != null) {
            return jpa.findByRoundResultIdAndParticipantId(query.roundResultId(), query.participantId())
                    .map(this::toDomain);
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
            return jpa.findByRoundResultId(query.roundResultId()).stream().map(this::toDomain).toList();
        }
        return List.of();
    }

    @Override
    public ParticipantRoundEntry update(ParticipantRoundEntryQuery query, ParticipantRoundEntryPatch patch) {
        ParticipantRoundEntryJpa entity = findJpa(query);
        if (patch.boostPurchased() != null) entity.setBoostPurchased(patch.boostPurchased());
        if (patch.discardedBarrelId() != null) entity.setDiscardedBarrelId(patch.discardedBarrelId());
        if (patch.totalScore() != null) entity.setTotalScore(patch.totalScore());
        if (patch.selectionTimestamp() != null) entity.setSelectionTimestamp(patch.selectionTimestamp());
        if (patch.selectionCount() != null) entity.setSelectionCount(patch.selectionCount());
        if (patch.rankInRound() != null) entity.setRankInRound(patch.rankInRound());
        return toDomain(jpa.save(entity));
    }

    @Override
    public int countByRoundResult(UUID roundResultId) {
        return jpa.countByRoundResultId(roundResultId);
    }

    private ParticipantRoundEntryJpa findJpa(ParticipantRoundEntryQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id())
                    .orElseThrow(() -> ApiException.notFound("ParticipantRoundEntry", query.id().toString()));
        }
        if (query.roundResultId() != null && query.participantId() != null) {
            return jpa.findByRoundResultIdAndParticipantId(query.roundResultId(), query.participantId())
                    .orElseThrow(() -> ApiException.notFound("ParticipantRoundEntry", buildDetail(query)));
        }
        throw ApiException.notFound("ParticipantRoundEntry", "unknown");
    }

    private String buildDetail(ParticipantRoundEntryQuery query) {
        if (query.id() != null) return query.id().toString();
        return "roundResult=" + query.roundResultId() + ", participant=" + query.participantId();
    }

    private ParticipantRoundEntry toDomain(ParticipantRoundEntryJpa e) {
        ParticipantRoundEntry entry = new ParticipantRoundEntry();
        entry.setId(e.getId());
        entry.setRoundResultId(e.getRoundResultId());
        entry.setParticipantId(e.getParticipantId());
        entry.setBoostPurchased(e.isBoostPurchased());
        entry.setDiscardedBarrelId(e.getDiscardedBarrelId());
        entry.setTotalScore(e.getTotalScore());
        entry.setSelectionTimestamp(e.getSelectionTimestamp());
        entry.setSelectionCount(e.getSelectionCount());
        entry.setRankInRound(e.getRankInRound());
        return entry;
    }

    private ParticipantRoundEntryJpa toJpa(ParticipantRoundEntry entry) {
        ParticipantRoundEntryJpa e = new ParticipantRoundEntryJpa();
        e.setId(entry.getId());
        e.setRoundResultId(entry.getRoundResultId());
        e.setParticipantId(entry.getParticipantId());
        e.setBoostPurchased(entry.isBoostPurchased());
        e.setDiscardedBarrelId(entry.getDiscardedBarrelId());
        e.setTotalScore(entry.getTotalScore());
        e.setSelectionTimestamp(entry.getSelectionTimestamp());
        e.setSelectionCount(entry.getSelectionCount());
        e.setRankInRound(entry.getRankInRound());
        return e;
    }
}
