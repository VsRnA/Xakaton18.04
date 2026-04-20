package com.vsrna.game.infrastructure.persistence.history;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.history.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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
        List<GameHistoryJpa> results = jpa.findByQuery(
                query.id(), query.gameRoomId(), query.winnerUserId(),
                query.from(), query.to(),
                PageRequest.of(0, 1)
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(toDomain(results.get(0)));
    }

    @Override
    public GameHistory get(GameHistoryQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("GameHistory", query.gameRoomId() != null
                        ? "room=" + query.gameRoomId()
                        : String.valueOf(query.id())));
    }

    @Override
    public List<GameHistory> list(GameHistoryQuery query) {
        Pageable pageable = query.size() == Integer.MAX_VALUE
                ? Pageable.unpaged()
                : PageRequest.of(Math.max(query.page(), 0), Math.max(query.size(), 1));
        return jpa.findByQuery(
                query.id(), query.gameRoomId(), query.winnerUserId(),
                query.from(), query.to(),
                pageable
        ).stream().map(this::toDomain).toList();
    }

    @Override
    public BigDecimal getCumulativeSystemBalance() {
        return Objects.requireNonNullElse(jpa.getSystemBalance(), BigDecimal.ZERO);
    }

    private GameHistory toDomain(GameHistoryJpa jpaEntity) {
        GameHistory history = new GameHistory();
        history.setId(jpaEntity.getId());
        history.setGameRoomId(jpaEntity.getGameRoomId());
        history.setWinnerUserId(jpaEntity.getWinnerUserId());
        history.setWinnerIsBot(jpaEntity.isWinnerIsBot());
        history.setPrizeAwarded(jpaEntity.getPrizeAwarded());
        history.setSystemRevenue(jpaEntity.getSystemRevenue());
        history.setCompletedAt(jpaEntity.getCompletedAt());
        history.setWinCriteria(jpaEntity.getWinCriteria());
        history.setSummaryJson(jpaEntity.getSummaryJson());
        history.setRealPlayersCount(jpaEntity.getRealPlayersCount());
        history.setBotCount(jpaEntity.getBotCount());
        history.setRealPlayersRevenue(jpaEntity.getRealPlayersRevenue());
        history.setBoostRevenue(jpaEntity.getBoostRevenue());
        history.setBoostUsedCount(jpaEntity.getBoostUsedCount());
        history.setWinnerUsedBoost(jpaEntity.isWinnerUsedBoost());
        return history;
    }

    private GameHistoryJpa toJpa(GameHistory history) {
        GameHistoryJpa jpaEntity = new GameHistoryJpa();
        jpaEntity.setId(history.getId());
        jpaEntity.setGameRoomId(history.getGameRoomId());
        jpaEntity.setWinnerUserId(history.getWinnerUserId());
        jpaEntity.setWinnerIsBot(history.isWinnerIsBot());
        jpaEntity.setPrizeAwarded(history.getPrizeAwarded());
        jpaEntity.setSystemRevenue(history.getSystemRevenue());
        jpaEntity.setWinCriteria(history.getWinCriteria());
        jpaEntity.setSummaryJson(history.getSummaryJson());
        jpaEntity.setRealPlayersCount(history.getRealPlayersCount());
        jpaEntity.setBotCount(history.getBotCount());
        jpaEntity.setRealPlayersRevenue(history.getRealPlayersRevenue());
        jpaEntity.setBoostRevenue(history.getBoostRevenue());
        jpaEntity.setBoostUsedCount(history.getBoostUsedCount());
        jpaEntity.setWinnerUsedBoost(history.isWinnerUsedBoost());
        return jpaEntity;
    }
}
