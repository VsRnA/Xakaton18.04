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
        applyPatch(entity, patch);
        return toDomain(jpa.save(entity));
    }

    private void applyPatch(ParticipantRoundEntryJpa entity, ParticipantRoundEntryPatch patch) {
        if (patch.boostPurchased() != null) entity.setBoostPurchased(patch.boostPurchased());
        if (patch.boostedBarrelId() != null) entity.setBoostedBarrelId(patch.boostedBarrelId());
        if (patch.totalScore() != null) entity.setTotalScore(patch.totalScore());
        if (patch.selectionTimestamp() != null) entity.setSelectionTimestamp(patch.selectionTimestamp());
        if (patch.selectionCount() != null) entity.setSelectionCount(patch.selectionCount());
        if (patch.rankInRound() != null) entity.setRankInRound(patch.rankInRound());
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

    private ParticipantRoundEntry toDomain(ParticipantRoundEntryJpa jpaEntity) {
        ParticipantRoundEntry entry = new ParticipantRoundEntry();
        entry.setId(jpaEntity.getId());
        entry.setRoundResultId(jpaEntity.getRoundResultId());
        entry.setParticipantId(jpaEntity.getParticipantId());
        entry.setBoostPurchased(jpaEntity.isBoostPurchased());
        entry.setBoostedBarrelId(jpaEntity.getBoostedBarrelId());
        entry.setTotalScore(jpaEntity.getTotalScore());
        entry.setSelectionTimestamp(jpaEntity.getSelectionTimestamp());
        entry.setSelectionCount(jpaEntity.getSelectionCount());
        entry.setRankInRound(jpaEntity.getRankInRound());
        return entry;
    }

    private ParticipantRoundEntryJpa toJpa(ParticipantRoundEntry entry) {
        ParticipantRoundEntryJpa jpaEntity = new ParticipantRoundEntryJpa();
        jpaEntity.setId(entry.getId());
        jpaEntity.setRoundResultId(entry.getRoundResultId());
        jpaEntity.setParticipantId(entry.getParticipantId());
        jpaEntity.setBoostPurchased(entry.isBoostPurchased());
        jpaEntity.setBoostedBarrelId(entry.getBoostedBarrelId());
        jpaEntity.setTotalScore(entry.getTotalScore());
        jpaEntity.setSelectionTimestamp(entry.getSelectionTimestamp());
        jpaEntity.setSelectionCount(entry.getSelectionCount());
        jpaEntity.setRankInRound(entry.getRankInRound());
        return jpaEntity;
    }
}
