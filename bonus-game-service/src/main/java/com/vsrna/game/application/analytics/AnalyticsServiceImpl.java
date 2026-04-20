package com.vsrna.game.application.analytics;

import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.domain.history.GameHistoryQuery;
import com.vsrna.game.domain.history.GameHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final GameHistoryRepository gameHistoryRepository;

    private static final int DEFAULT_PERIOD_DAYS = 30;

    private Instant resolveEffectiveTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    private Instant resolveEffectiveFrom(Instant from, Instant effectiveTo) {
        return from != null ? from : effectiveTo.minus(DEFAULT_PERIOD_DAYS, ChronoUnit.DAYS);
    }

    @Override
    @Transactional(readOnly = true)
    public GameAnalyticsSummary getSummary(Instant from, Instant to) {
        Instant effectiveTo = resolveEffectiveTo(to);
        Instant effectiveFrom = resolveEffectiveFrom(from, effectiveTo);
        List<GameHistory> games = gameHistoryRepository.list(GameHistoryQuery.byPeriod(effectiveFrom, effectiveTo));
        BigDecimal cumulativeBalance = gameHistoryRepository.getCumulativeSystemBalance();

        long totalGames = games.size();
        if (totalGames == 0) {
            return emptyResult(effectiveFrom, effectiveTo, cumulativeBalance);
        }

        BigDecimal totalRealRevenue = sum(games, GameHistory::getRealPlayersRevenue);
        BigDecimal totalPrizesAwarded = sum(games, GameHistory::getPrizeAwarded);
        BigDecimal totalBoostRevenue = sum(games, GameHistory::getBoostRevenue);
        BigDecimal totalRetained = totalRealRevenue.add(totalBoostRevenue).subtract(totalPrizesAwarded);
        double retentionRate = totalRealRevenue.add(totalBoostRevenue).compareTo(BigDecimal.ZERO) > 0
                ? totalRetained.divide(totalRealRevenue.add(totalBoostRevenue), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        long botWinsCount = games.stream().filter(GameHistory::isWinnerIsBot).count();
        long realWinsCount = totalGames - botWinsCount;
        double botWinRate = pct(botWinsCount, totalGames);

        double avgRealPlayers = games.stream()
                .mapToInt(GameHistory::getRealPlayersCount)
                .average().orElse(0.0);
        double avgBotFillRate = games.stream()
                .mapToDouble(g -> g.getRealPlayersCount() + g.getBotCount() > 0
                        ? (double) g.getBotCount() / (g.getRealPlayersCount() + g.getBotCount())
                        : 0.0)
                .average().orElse(0.0) * 100;

        long uniqueWinners = games.stream()
                .filter(game -> !game.isWinnerIsBot() && game.getWinnerUserId() != null)
                .map(GameHistory::getWinnerUserId)
                .distinct()
                .count();
        long gamesWithBoost = games.stream().filter(game -> game.getBoostUsedCount() > 0).count();
        double boostUsageRate = pct(gamesWithBoost, totalGames);

        long realWinsWithBoost = games.stream()
                .filter(game -> !game.isWinnerIsBot() && game.isWinnerUsedBoost())
                .count();
        double winnerBoostRate = realWinsCount > 0 ? pct(realWinsWithBoost, realWinsCount) : 0.0;

        return new GameAnalyticsSummary(
                effectiveFrom, effectiveTo,
                totalGames,
                totalRealRevenue, totalPrizesAwarded, totalBoostRevenue,
                totalRetained, retentionRate, cumulativeBalance,
                botWinsCount, realWinsCount, botWinRate,
                avgRealPlayers, avgBotFillRate,
                uniqueWinners, boostUsageRate,
                winnerBoostRate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getTimeSeries(Instant from, Instant to) {
        Instant effectiveTo = resolveEffectiveTo(to);
        Instant effectiveFrom = resolveEffectiveFrom(from, effectiveTo);
        List<GameHistory> games = gameHistoryRepository.list(GameHistoryQuery.byPeriod(effectiveFrom, effectiveTo));

        Map<LocalDate, List<GameHistory>> byDay = games.stream()
                .collect(Collectors.groupingBy(game ->
                        game.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate()));

        List<LocalDate> allDays = new ArrayList<>();
        LocalDate cursor = effectiveFrom.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDay = effectiveTo.atZone(ZoneOffset.UTC).toLocalDate();
        while (!cursor.isAfter(toDay)) {
            allDays.add(cursor);
            cursor = cursor.plusDays(1);
        }

        return allDays.stream().map(day -> {
            List<GameHistory> dayGames = byDay.getOrDefault(day, List.of());
            BigDecimal revenue = sum(dayGames, GameHistory::getRealPlayersRevenue);
            BigDecimal prizes = sum(dayGames, GameHistory::getPrizeAwarded);
            long dayBotWins = dayGames.stream().filter(GameHistory::isWinnerIsBot).count();
            long dayRealWins = dayGames.size() - dayBotWins;
            return new TimeSeriesPoint(day, dayGames.size(), revenue, prizes,
                    revenue.subtract(prizes), dayBotWins, dayRealWins);
        }).toList();
    }

    private BigDecimal sum(List<GameHistory> games,
                           java.util.function.Function<GameHistory, BigDecimal> getter) {
        return games.stream()
                .map(game -> {
                    BigDecimal value = getter.apply(game);
                    return value != null ? value : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double pct(long part, long total) {
        return total > 0 ? BigDecimal.valueOf(part)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
    }

    private GameAnalyticsSummary emptyResult(Instant from, Instant to, BigDecimal cumulativeBalance) {
        return new GameAnalyticsSummary(
                from, to, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0.0, cumulativeBalance,
                0, 0, 0.0, 0.0, 0.0,
                0, 0.0, 0.0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameHistory> listGames(Instant from, Instant to, int page, int size) {
        Instant effectiveTo = resolveEffectiveTo(to);
        Instant effectiveFrom = resolveEffectiveFrom(from, effectiveTo);
        return gameHistoryRepository.list(GameHistoryQuery.byPeriod(effectiveFrom, effectiveTo, page, size));
    }
}
