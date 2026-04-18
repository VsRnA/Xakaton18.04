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
        if (query.status() != null) {
            return jpa.findByStatus(query.status(), pageable).stream().map(this::toDomain).toList();
        }
        return jpa.findAll(pageable).getContent().stream().map(this::toDomain).toList();
    }

    @Override
    public GameRoom update(GameRoomQuery query, GameRoomPatch patch) {
        GameRoomJpa entity = findJpa(query);
        if (patch.status() != null) entity.setStatus(patch.status());
        if (patch.currentPlayerCount() != null) entity.setCurrentPlayerCount(patch.currentPlayerCount());
        if (patch.prizePoolAmount() != null) entity.setPrizePoolAmount(patch.prizePoolAmount());
        if (patch.startedAt() != null) entity.setStartedAt(patch.startedAt());
        if (patch.finishedAt() != null) entity.setFinishedAt(patch.finishedAt());
        return toDomain(jpa.save(entity));
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

    private GameRoom toDomain(GameRoomJpa e) {
        GameRoom room = new GameRoom();
        room.setId(e.getId());
        room.setStatus(e.getStatus());
        room.setCreatedByUserId(e.getCreatedByUserId());
        room.setCreatedAt(e.getCreatedAt());
        room.setStartedAt(e.getStartedAt());
        room.setFinishedAt(e.getFinishedAt());
        room.setCurrentPlayerCount(e.getCurrentPlayerCount());
        room.setPrizePoolAmount(e.getPrizePoolAmount());
        return room;
    }

    private GameRoomJpa toJpa(GameRoom room) {
        GameRoomJpa e = new GameRoomJpa();
        e.setId(room.getId());
        e.setStatus(room.getStatus());
        e.setCreatedByUserId(room.getCreatedByUserId());
        e.setStartedAt(room.getStartedAt());
        e.setFinishedAt(room.getFinishedAt());
        e.setCurrentPlayerCount(room.getCurrentPlayerCount());
        e.setPrizePoolAmount(room.getPrizePoolAmount());
        return e;
    }
}
