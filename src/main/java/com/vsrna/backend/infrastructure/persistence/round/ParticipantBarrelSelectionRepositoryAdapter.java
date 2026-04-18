package com.vsrna.backend.infrastructure.persistence.round;

import com.vsrna.backend.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ParticipantBarrelSelectionRepositoryAdapter implements ParticipantBarrelSelectionRepository {

    private final ParticipantBarrelSelectionJpaRepository jpa;

    @Override
    public List<ParticipantBarrelSelection> createAll(List<ParticipantBarrelSelection> selections) {
        return jpa.saveAll(selections);
    }

    @Override
    public List<ParticipantBarrelSelection> list(ParticipantBarrelSelectionQuery query) {
        if (query.entryId() != null) {
            return jpa.findByIdEntryId(query.entryId());
        }
        return List.of();
    }

    @Override
    @Transactional
    public void delete(ParticipantBarrelSelectionQuery query) {
        if (query.entryId() != null) {
            jpa.deleteByIdEntryId(query.entryId());
        }
    }
}
