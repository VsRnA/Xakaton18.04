package com.vsrna.game.infrastructure.persistence.history;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.history.*;
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
        return toDomain(jpa.save(toJpa(gameHistory)));
    }

    @Override
    public Optional<GameHistory> find(GameHistoryQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id()).map(this::toDomain);
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId()).map(this::toDomain);
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
            return jpa.findByWinnerUserId(query.winnerUserId(), PageRequest.of(page, size))
                    .stream().map(this::toDomain).toList();
        }
        return jpa.findAll(PageRequest.of(page, size)).getContent().stream().map(this::toDomain).toList();
    }

    private GameHistory toDomain(GameHistoryJpa e) {
        GameHistory h = new GameHistory();
        h.setId(e.getId());
        h.setGameRoomId(e.getGameRoomId());
        h.setWinnerUserId(e.getWinnerUserId());
        h.setWinnerIsBot(e.isWinnerIsBot());
        h.setPrizeAwarded(e.getPrizeAwarded());
        h.setSystemRevenue(e.getSystemRevenue());
        h.setCompletedAt(e.getCompletedAt());
        h.setWinCriteria(e.getWinCriteria());
        h.setSummaryJson(e.getSummaryJson());
        return h;
    }

    private GameHistoryJpa toJpa(GameHistory h) {
        GameHistoryJpa e = new GameHistoryJpa();
        e.setId(h.getId());
        e.setGameRoomId(h.getGameRoomId());
        e.setWinnerUserId(h.getWinnerUserId());
        e.setWinnerIsBot(h.isWinnerIsBot());
        e.setPrizeAwarded(h.getPrizeAwarded());
        e.setSystemRevenue(h.getSystemRevenue());
        e.setWinCriteria(h.getWinCriteria());
        e.setSummaryJson(h.getSummaryJson());
        return e;
    }
}
