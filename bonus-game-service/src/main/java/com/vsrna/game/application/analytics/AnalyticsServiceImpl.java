package com.vsrna.game.application.analytics;

import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.domain.history.GameHistoryAnalyticsRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final GameHistoryRepository gameHistoryRepository;
    private final GameHistoryAnalyticsRepository analyticsRepository;

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
        List<GameHistory> games = gameHistoryRepository.list(GameHistoryQuery.allByPeriod(effectiveFrom, effectiveTo));
        BigDecimal cumulativeBalance = analyticsRepository.getCumulativeSystemBalance();

        if (games.isEmpty()) {
            return emptyResult(effectiveFrom, effectiveTo, cumulativeBalance);
        }

        GameAnalyticsEconomics economics = calcEconomics(games, cumulativeBalance);
        GameAnalyticsRooms rooms = calcRooms(games);
        GameAnalyticsPlayers players = calcPlayers(games, rooms.realPlayerWins());

        return new GameAnalyticsSummary(effectiveFrom, effectiveTo, economics, rooms, players);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getTimeSeries(Instant from, Instant to) {
        Instant effectiveTo = resolveEffectiveTo(to);
        Instant effectiveFrom = resolveEffectiveFrom(from, effectiveTo);
        List<GameHistory> games = gameHistoryRepository.list(GameHistoryQuery.allByPeriod(effectiveFrom, effectiveTo));

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

    @Override
    @Transactional(readOnly = true)
    public List<GameHistoryEntry> listGames(Instant from, Instant to, int page, int size) {
        Instant effectiveTo = resolveEffectiveTo(to);
        Instant effectiveFrom = resolveEffectiveFrom(from, effectiveTo);
        return gameHistoryRepository.list(GameHistoryQuery.byPeriod(effectiveFrom, effectiveTo, page, size))
                .stream()
                .map(this::toEntry)
                .toList();
    }

    private GameAnalyticsEconomics calcEconomics(List<GameHistory> games, BigDecimal cumulativeBalance) {
        BigDecimal totalRealRevenue = sum(games, GameHistory::getRealPlayersRevenue);
        BigDecimal totalPrizesAwarded = sum(games, GameHistory::getPrizeAwarded);
        BigDecimal totalBoostRevenue = sum(games, GameHistory::getBoostRevenue);
        BigDecimal totalRetained = totalRealRevenue.add(totalBoostRevenue).subtract(totalPrizesAwarded);
        double retentionRate = calcRetentionRate(totalRealRevenue, totalBoostRevenue, totalPrizesAwarded);
        return new GameAnalyticsEconomics(
                totalRealRevenue, totalPrizesAwarded, totalBoostRevenue,
                totalRetained, retentionRate, cumulativeBalance);
    }

    private GameAnalyticsRooms calcRooms(List<GameHistory> games) {
        long totalGames = games.size();
        long botWins = games.stream().filter(GameHistory::isWinnerIsBot).count();
        long realWins = totalGames - botWins;
        double botWinRate = pct(botWins, totalGames);
        double avgRealPlayers = games.stream().mapToInt(GameHistory::getRealPlayersCount).average().orElse(0.0);
        double avgBotFillRate = games.stream().mapToDouble(GameHistory::getBotFillRate).average().orElse(0.0) * 100;
        return new GameAnalyticsRooms(totalGames, botWins, realWins, botWinRate, avgRealPlayers, avgBotFillRate);
    }

    private GameAnalyticsPlayers calcPlayers(List<GameHistory> games, long realWins) {
        long uniqueWinners = games.stream()
                .filter(g -> !g.isWinnerIsBot() && g.getWinnerUserId() != null)
                .map(GameHistory::getWinnerUserId)
                .distinct()
                .count();
        long gamesWithBoost = games.stream().filter(g -> g.getBoostUsedCount() > 0).count();
        double boostUsageRate = pct(gamesWithBoost, games.size());
        long realWinsWithBoost = games.stream()
                .filter(g -> !g.isWinnerIsBot() && g.isWinnerUsedBoost())
                .count();
        double winnerBoostRate = realWins > 0 ? pct(realWinsWithBoost, realWins) : 0.0;
        return new GameAnalyticsPlayers(uniqueWinners, boostUsageRate, winnerBoostRate);
    }

    private double calcRetentionRate(BigDecimal realRevenue, BigDecimal boostRevenue, BigDecimal prizesAwarded) {
        BigDecimal totalIn = realRevenue.add(boostRevenue);
        if (totalIn.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return totalIn.subtract(prizesAwarded)
                .divide(totalIn, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private GameHistoryEntry toEntry(GameHistory game) {
        return new GameHistoryEntry(
                game.getGameRoomId(), game.getCompletedAt(), game.getWinnerUserId(),
                game.isWinnerIsBot(), game.getEntryFeeAmount(), game.getRealPlayersRevenue(),
                game.getPrizeAwarded(), game.getSystemBalance(), game.getWinCriteria(),
                game.getRealPlayersCount(), game.getBotCount(),
                game.isBoostAvailable(), game.getBoostUsedCount(), game.getBoostRevenue());
    }

    private GameAnalyticsSummary emptyResult(Instant from, Instant to, BigDecimal cumulativeBalance) {
        return new GameAnalyticsSummary(
                from, to,
                new GameAnalyticsEconomics(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, 0.0, cumulativeBalance),
                new GameAnalyticsRooms(0, 0, 0, 0.0, 0.0, 0.0),
                new GameAnalyticsPlayers(0, 0.0, 0.0));
    }

    private BigDecimal sum(List<GameHistory> games, Function<GameHistory, BigDecimal> getter) {
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
}
