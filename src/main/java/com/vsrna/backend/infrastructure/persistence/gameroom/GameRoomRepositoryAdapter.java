package com.vsrna.backend.infrastructure.persistence.gameroom;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.gameroom.*;
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
    public GameRoom create(GameRoom gameRoom) {
        return jpa.save(gameRoom);
    }

    @Override
    public Optional<GameRoom> find(GameRoomQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id());
        }
        return Optional.empty();
    }

    @Override
    public GameRoom get(GameRoomQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("GameRoom", query.id() != null ? query.id().toString() : "unknown"));
    }

    @Override
    public List<GameRoom> list(GameRoomQuery query) {
        int page = Math.max(query.page(), 0);
        int size = query.size() > 0 ? query.size() : 20;
        PageRequest pageable = PageRequest.of(page, size);
        if (query.status() != null) {
            return jpa.findByStatus(query.status(), pageable);
        }
        return jpa.findAll(pageable).getContent();
    }

    @Override
    public GameRoom update(GameRoomQuery query, GameRoomPatch patch) {
        GameRoom room = get(query);
        if (patch.status() != null) room.setStatus(patch.status());
        if (patch.currentPlayerCount() != null) room.setCurrentPlayerCount(patch.currentPlayerCount());
        if (patch.prizePoolAmount() != null) room.setPrizePoolAmount(patch.prizePoolAmount());
        if (patch.startedAt() != null) room.setStartedAt(patch.startedAt());
        if (patch.finishedAt() != null) room.setFinishedAt(patch.finishedAt());
        return jpa.save(room);
    }

    @Override
    public void delete(GameRoomQuery query) {
        find(query).ifPresent(room -> jpa.deleteById(room.getId()));
    }
}
