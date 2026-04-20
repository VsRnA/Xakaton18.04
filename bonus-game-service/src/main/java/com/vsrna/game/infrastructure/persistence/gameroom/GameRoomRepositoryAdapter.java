package com.vsrna.game.infrastructure.persistence.gameroom;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameRoomRepositoryAdapter implements GameRoomRepository {

    private final GameRoomJpaRepository jpa;

    @Override
    public GameRoom create(GameRoom room) {
        return toDomain(jpa.save(toJpa(room)));
    }

    @Override
    public Optional<GameRoom> find(GameRoomQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id()).map(this::toDomain);
        }
        return Optional.empty();
    }

    @Override
    public GameRoom get(GameRoomQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("GameRoom", query.id() != null ? query.id().toString() : "unknown"));
    }

    @Override
    public GameRoom getForUpdate(GameRoomQuery query) {
        if (query.id() == null) {
            throw ApiException.notFound("GameRoom", "unknown");
        }
        return jpa.findByIdForUpdate(query.id())
                .map(this::toDomain)
                .orElseThrow(() -> ApiException.notFound("GameRoom", query.id().toString()));
    }

    @Override
    public List<GameRoom> list(GameRoomQuery query) {
        int page = Math.max(query.page(), 0);
        int size = query.size() > 0 ? query.size() : 20;
        PageRequest pageable = PageRequest.of(page, size);

        boolean hasFilters = query.entryFeeMin() != null || query.entryFeeMax() != null
                || query.maxPlayersFilter() != null || Boolean.TRUE.equals(query.onlyWithSlots());
        if (hasFilters) {
            boolean onlyWithSlots = Boolean.TRUE.equals(query.onlyWithSlots());
            return jpa.findFiltered(query.status(), query.entryFeeMin(), query.entryFeeMax(),
                    query.maxPlayersFilter(), onlyWithSlots, pageable)
                    .stream().map(this::toDomain).toList();
        }
        if (query.status() != null) {
            return jpa.findByStatus(query.status(), pageable).stream().map(this::toDomain).toList();
        }
        return jpa.findAll(pageable).getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public GameRoom update(GameRoomQuery query, GameRoomPatch patch) {
        GameRoomJpa entity = findJpa(query);
        applyPatch(entity, patch);
        return toDomain(jpa.save(entity));
    }

    private void applyPatch(GameRoomJpa entity, GameRoomPatch patch) {
        if (patch.status() != null) entity.setStatus(patch.status());
        if (patch.currentPlayerCount() != null) entity.setCurrentPlayerCount(patch.currentPlayerCount());
        if (patch.prizePoolAmount() != null) entity.setPrizePoolAmount(patch.prizePoolAmount());
        if (patch.startedAt() != null) entity.setStartedAt(patch.startedAt());
        if (patch.finishedAt() != null) entity.setFinishedAt(patch.finishedAt());
        if (patch.waitTimerExpiresAt() != null) entity.setWaitTimerExpiresAt(patch.waitTimerExpiresAt());
    }

    @Override
    public void delete(GameRoomQuery query) {
        findJpaOptional(query).ifPresent(e -> jpa.deleteById(e.getId()));
    }

    private GameRoomJpa findJpa(GameRoomQuery query) {
        return findJpaOptional(query).orElseThrow(() ->
                ApiException.notFound("GameRoom", query.id() != null ? query.id().toString() : "unknown"));
    }

    private Optional<GameRoomJpa> findJpaOptional(GameRoomQuery query) {
        if (query.id() != null) return jpa.findById(query.id());
        return Optional.empty();
    }

    private GameRoom toDomain(GameRoomJpa jpaEntity) {
        GameRoom room = new GameRoom();
        room.setId(jpaEntity.getId());
        room.setStatus(jpaEntity.getStatus());
        room.setCreatedByUserId(jpaEntity.getCreatedByUserId());
        room.setCreatedAt(jpaEntity.getCreatedAt());
        room.setStartedAt(jpaEntity.getStartedAt());
        room.setFinishedAt(jpaEntity.getFinishedAt());
        room.setWaitTimerExpiresAt(jpaEntity.getWaitTimerExpiresAt());
        room.setCurrentPlayerCount(jpaEntity.getCurrentPlayerCount());
        room.setPrizePoolAmount(jpaEntity.getPrizePoolAmount());
        return room;
    }

    private GameRoomJpa toJpa(GameRoom room) {
        GameRoomJpa jpaEntity = new GameRoomJpa();
        jpaEntity.setId(room.getId());
        jpaEntity.setStatus(room.getStatus());
        jpaEntity.setCreatedByUserId(room.getCreatedByUserId());
        jpaEntity.setStartedAt(room.getStartedAt());
        jpaEntity.setFinishedAt(room.getFinishedAt());
        jpaEntity.setWaitTimerExpiresAt(room.getWaitTimerExpiresAt());
        jpaEntity.setCurrentPlayerCount(room.getCurrentPlayerCount());
        jpaEntity.setPrizePoolAmount(room.getPrizePoolAmount());
        return jpaEntity;
    }
}
