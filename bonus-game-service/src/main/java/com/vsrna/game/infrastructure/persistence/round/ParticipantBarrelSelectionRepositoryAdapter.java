package com.vsrna.game.infrastructure.persistence.round;

import com.vsrna.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ParticipantBarrelSelectionRepositoryAdapter implements ParticipantBarrelSelectionRepository {

    private final ParticipantBarrelSelectionJpaRepository jpa;

    @Override
    public List<ParticipantBarrelSelection> createAll(List<ParticipantBarrelSelection> selections) {
        return jpa.saveAll(selections.stream().map(this::toJpa).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<ParticipantBarrelSelection> list(ParticipantBarrelSelectionQuery query) {
        if (query.entryId() != null) {
            return jpa.findByIdEntryId(query.entryId()).stream().map(this::toDomain).toList();
        }
        return List.of();
    }

    @Override
    public List<ParticipantBarrelSelection> listByEntries(List<UUID> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) return List.of();
        return jpa.findByEntryIds(entryIds).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(ParticipantBarrelSelectionQuery query) {
        if (query.entryId() != null) {
            jpa.deleteByIdEntryId(query.entryId());
        }
    }

    private ParticipantBarrelSelection toDomain(ParticipantBarrelSelectionJpa e) {
        return new ParticipantBarrelSelection(e.getEntryId(), e.getBarrelId());
    }

    private ParticipantBarrelSelectionJpa toJpa(ParticipantBarrelSelection s) {
        return new ParticipantBarrelSelectionJpa(s.getEntryId(), s.getBarrelId());
    }
}
