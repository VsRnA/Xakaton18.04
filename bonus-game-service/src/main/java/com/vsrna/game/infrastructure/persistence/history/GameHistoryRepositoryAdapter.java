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
        BigDecimal totalRevenue = Objects.requireNonNullElse(jpa.sumRealPlayersRevenueAll(), BigDecimal.ZERO);
        BigDecimal totalPaid = Objects.requireNonNullElse(jpa.sumPrizeAwardedAll(), BigDecimal.ZERO);
        return totalRevenue.subtract(totalPaid);
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
        h.setRealPlayersCount(e.getRealPlayersCount());
        h.setBotCount(e.getBotCount());
        h.setRealPlayersRevenue(e.getRealPlayersRevenue());
        h.setBoostRevenue(e.getBoostRevenue());
        h.setBoostUsedCount(e.getBoostUsedCount());
        h.setWinnerUsedBoost(e.isWinnerUsedBoost());
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
        e.setRealPlayersCount(h.getRealPlayersCount());
        e.setBotCount(h.getBotCount());
        e.setRealPlayersRevenue(h.getRealPlayersRevenue());
        e.setBoostRevenue(h.getBoostRevenue());
        e.setBoostUsedCount(h.getBoostUsedCount());
        e.setWinnerUsedBoost(h.isWinnerUsedBoost());
        return e;
    }
}
