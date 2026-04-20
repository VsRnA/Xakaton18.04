package com.vsrna.game.application.analytics;

import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.domain.history.GameHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final GameHistoryRepository gameHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public GameAnalyticsSummary getSummary(Instant from, Instant to) {
        List<GameHistory> games = gameHistoryRepository.listByPeriod(from, to);
        BigDecimal cumulativeBalance = gameHistoryRepository.getCumulativeSystemBalance();

        long totalGames = games.size();
        if (totalGames == 0) {
            return emptyResult(from, to, cumulativeBalance);
        }

        BigDecimal totalRealRevenue = sum(games, GameHistory::getRealPlayersRevenue);
        BigDecimal totalPrizesAwarded = sum(games, GameHistory::getPrizeAwarded);
        BigDecimal totalBoostRevenue = sum(games, GameHistory::getBoostRevenue);
        BigDecimal totalRetained = totalRealRevenue.add(totalBoostRevenue).subtract(totalPrizesAwarded);
        double retentionRate = totalRealRevenue.add(totalBoostRevenue).compareTo(BigDecimal.ZERO) > 0
                ? totalRetained.divide(totalRealRevenue.add(totalBoostRevenue), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        long botWins = games.stream().filter(GameHistory::isWinnerIsBot).count();
        long realWins = totalGames - botWins;
        double botWinRate = pct(botWins, totalGames);

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
                .collect(Collectors.toSet())
                .size();
        long gamesWithBoost = games.stream().filter(game -> game.getBoostUsedCount() > 0).count();
        double boostUsageRate = pct(gamesWithBoost, totalGames);

        long realWinsWithBoost = games.stream()
                .filter(game -> !game.isWinnerIsBot() && game.isWinnerUsedBoost())
                .count();
        double winnerBoostRate = realWins > 0 ? pct(realWinsWithBoost, realWins) : 0.0;

        return new GameAnalyticsSummary(
                from, to,
                totalGames,
                totalRealRevenue, totalPrizesAwarded, totalBoostRevenue,
                totalRetained, retentionRate, cumulativeBalance,
                botWins, realWins, botWinRate,
                avgRealPlayers, avgBotFillRate,
                uniqueWinners, boostUsageRate,
                winnerBoostRate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPoint> getTimeSeries(Instant from, Instant to) {
        List<GameHistory> games = gameHistoryRepository.listByPeriod(from, to);

        Map<LocalDate, List<GameHistory>> byDay = games.stream()
                .collect(Collectors.groupingBy(game ->
                        game.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate()));

        List<LocalDate> allDays = new ArrayList<>();
        LocalDate cursor = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDay = to.atZone(ZoneOffset.UTC).toLocalDate();
        while (!cursor.isAfter(toDay)) {
            allDays.add(cursor);
            cursor = cursor.plusDays(1);
        }

        return allDays.stream().map(day -> {
            List<GameHistory> dayGames = byDay.getOrDefault(day, List.of());
            BigDecimal revenue = sum(dayGames, GameHistory::getRealPlayersRevenue);
            BigDecimal prizes = sum(dayGames, GameHistory::getPrizeAwarded);
            long bWins = dayGames.stream().filter(GameHistory::isWinnerIsBot).count();
            long rWins = dayGames.size() - bWins;
            return new TimeSeriesPoint(day, dayGames.size(), revenue, prizes,
                    revenue.subtract(prizes), bWins, rWins);
        }).toList();
    }

    private BigDecimal sum(List<GameHistory> games,
                           java.util.function.Function<GameHistory, BigDecimal> getter) {
        return games.stream()
                .map(game -> getter.apply(game) != null ? getter.apply(game) : BigDecimal.ZERO)
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
        List<GameHistory> all = gameHistoryRepository.listByPeriod(from, to);
        int start = page * size;
        if (start >= all.size()) return List.of();
        return all.subList(start, Math.min(start + size, all.size()));
    }
}
