package com.vsrna.backend.infrastructure.persistence.history;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.history.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameHistoryRepositoryAdapter implements GameHistoryRepository {

    private final GameHistoryJpaRepository jpa;

    @Override
    public GameHistory create(GameHistory gameHistory) {
        return jpa.save(gameHistory);
    }

    @Override
    public Optional<GameHistory> find(GameHistoryQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id());
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId());
        }
        return Optional.empty();
    }

    @Override
    public GameHistory get(GameHistoryQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("GameHistory", query.gameRoomId() != null
                        ? "room=" + query.gameRoomId() : query.id().toString()));
    }

    @Override
    public List<GameHistory> list(GameHistoryQuery query) {
        int page = Math.max(query.page(), 0);
        int size = query.size() > 0 ? query.size() : 20;
        if (query.winnerUserId() != null) {
            return jpa.findByWinnerUserId(query.winnerUserId(), PageRequest.of(page, size));
        }
        return jpa.findAll(PageRequest.of(page, size)).getContent();
    }
}
