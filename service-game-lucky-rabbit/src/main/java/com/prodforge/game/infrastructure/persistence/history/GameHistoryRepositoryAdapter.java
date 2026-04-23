package com.prodforge.game.infrastructure.persistence.history;

import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.history.GameHistory;
import com.prodforge.game.domain.history.GameHistoryAnalyticsRepository;
import com.prodforge.game.domain.history.GameHistoryQuery;
import com.prodforge.game.domain.history.GameHistoryRepository;
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
public class GameHistoryRepositoryAdapter implements GameHistoryRepository, GameHistoryAnalyticsRepository {

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
        Pageable pageable = query.isUnbounded()
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

    private GameHistory toDomain(GameHistoryJpa e) {
        return GameHistory.builder()
                .id(e.getId())
                .gameRoomId(e.getGameRoomId())
                .winnerUserId(e.getWinnerUserId())
                .winnerIsBot(e.isWinnerIsBot())
                .prizeAwarded(e.getPrizeAwarded())
                .systemRevenue(e.getSystemRevenue())
                .completedAt(e.getCompletedAt())
                .winCriteria(e.getWinCriteria())
                .summaryJson(e.getSummaryJson())
                .realPlayersCount(e.getRealPlayersCount())
                .botCount(e.getBotCount())
                .realPlayersRevenue(e.getRealPlayersRevenue())
                .boostRevenue(e.getBoostRevenue())
                .boostUsedCount(e.getBoostUsedCount())
                .winnerUsedBoost(e.isWinnerUsedBoost())
                .entryFeeAmount(e.getEntryFeeAmount())
                .boostAvailable(e.isBoostAvailable())
                .build();
    }

    private GameHistoryJpa toJpa(GameHistory h) {
        GameHistoryJpa e = new GameHistoryJpa();
        e.setId(h.getId());
        e.setGameRoomId(h.getGameRoomId());
        e.setWinnerUserId(h.getWinnerUserId());
        e.setWinnerIsBot(h.isWinnerIsBot());
        e.setPrizeAwarded(h.getPrizeAwarded());
        e.setSystemRevenue(h.getSystemRevenue());
        e.setCompletedAt(h.getCompletedAt());
        e.setWinCriteria(h.getWinCriteria());
        e.setSummaryJson(h.getSummaryJson());
        e.setRealPlayersCount(h.getRealPlayersCount());
        e.setBotCount(h.getBotCount());
        e.setRealPlayersRevenue(h.getRealPlayersRevenue());
        e.setBoostRevenue(h.getBoostRevenue());
        e.setBoostUsedCount(h.getBoostUsedCount());
        e.setWinnerUsedBoost(h.isWinnerUsedBoost());
        e.setEntryFeeAmount(h.getEntryFeeAmount());
        e.setBoostAvailable(h.isBoostAvailable());
        return e;
    }
}
