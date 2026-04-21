package com.vsrna.game.infrastructure.persistence.participant;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.participant.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameParticipantRepositoryAdapter implements GameParticipantRepository {

    private final GameParticipantJpaRepository jpa;

    @Override
    public GameParticipant create(GameParticipant participant) {
        return toDomain(jpa.save(toJpa(participant)));
    }

    @Override
    public Optional<GameParticipant> find(GameParticipantQuery query) {
        return jpa.findByQuery(query.id(), query.gameRoomId(), query.userId(), query.status())
                .stream().findFirst().map(this::toDomain);
    }

    @Override
    public GameParticipant get(GameParticipantQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("GameParticipant", buildDetail(query)));
    }

    @Override
    public List<GameParticipant> list(GameParticipantQuery query) {
        return jpa.findByQuery(query.id(), query.gameRoomId(), query.userId(), query.status())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public int count(GameParticipantQuery query) {
        if (query.gameRoomId() != null) {
            return jpa.countByGameRoomId(query.gameRoomId());
        }
        return 0;
    }

    @Override
    public GameParticipant update(GameParticipantQuery query, GameParticipantPatch patch) {
        GameParticipantJpa entity = findJpa(query);
        applyPatch(entity, patch);
        return toDomain(jpa.save(entity));
    }

    private void applyPatch(GameParticipantJpa entity, GameParticipantPatch patch) {
        if (patch.status() != null) entity.setStatus(patch.status());
        if (patch.advancedToFinal() != null) entity.setAdvancedToFinal(patch.advancedToFinal());
        if (patch.reservedPoints() != null) entity.setReservedPoints(patch.reservedPoints());
        if (patch.displayName() != null) entity.setDisplayName(patch.displayName());
        if (patch.round2Ready() != null) entity.setRound2Ready(patch.round2Ready());
    }

    private GameParticipantJpa findJpa(GameParticipantQuery query) {
        return jpa.findByQuery(query.id(), query.gameRoomId(), query.userId(), query.status())
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("GameParticipant", buildDetail(query)));
    }

    private String buildDetail(GameParticipantQuery query) {
        if (query.id() != null) return query.id().toString();
        if (query.gameRoomId() != null && query.userId() != null)
            return "room=" + query.gameRoomId() + ", user=" + query.userId();
        return "unknown";
    }

    private GameParticipant toDomain(GameParticipantJpa jpaEntity) {
        GameParticipant participant = new GameParticipant();
        participant.setId(jpaEntity.getId());
        participant.setGameRoomId(jpaEntity.getGameRoomId());
        participant.setUserId(jpaEntity.getUserId());
        participant.setBot(jpaEntity.isBot());
        participant.setDisplayName(jpaEntity.getDisplayName());
        participant.setReservedPoints(jpaEntity.getReservedPoints());
        participant.setStatus(jpaEntity.getStatus());
        participant.setAdvancedToFinal(jpaEntity.isAdvancedToFinal());
        participant.setRound2Ready(jpaEntity.isRound2Ready());
        participant.setJoinedAt(jpaEntity.getJoinedAt());
        return participant;
    }

    private GameParticipantJpa toJpa(GameParticipant participant) {
        GameParticipantJpa jpaEntity = new GameParticipantJpa();
        jpaEntity.setId(participant.getId());
        jpaEntity.setGameRoomId(participant.getGameRoomId());
        jpaEntity.setUserId(participant.getUserId());
        jpaEntity.setBot(participant.isBot());
        jpaEntity.setDisplayName(participant.getDisplayName());
        jpaEntity.setReservedPoints(participant.getReservedPoints());
        jpaEntity.setStatus(participant.getStatus());
        jpaEntity.setAdvancedToFinal(participant.isAdvancedToFinal());
        jpaEntity.setRound2Ready(participant.isRound2Ready());
        return jpaEntity;
    }
}
