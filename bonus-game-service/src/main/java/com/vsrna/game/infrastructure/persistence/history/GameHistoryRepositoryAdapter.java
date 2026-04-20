package com.vsrna.game.infrastructure.persistence.history;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.history.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Override
    public List<GameHistory> listByPeriod(Instant from, Instant to) {
        return jpa.findByCompletedAtBetween(from, to).stream().map(this::toDomain).toList();
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
