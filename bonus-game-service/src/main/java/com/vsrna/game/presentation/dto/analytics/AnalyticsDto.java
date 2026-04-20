package com.vsrna.game.presentation.dto.analytics;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vsrna.game.application.analytics.GameAnalyticsSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AnalyticsDto {

    public record AnalyticsRequest(
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
            Instant from,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
            Instant to
    ) {}

    public record EconomicsBlock(
            BigDecimal totalRealRevenue,
            BigDecimal totalPrizesAwarded,
            BigDecimal totalBoostRevenue,
            BigDecimal totalRetained,
            double retentionRatePercent,
            BigDecimal cumulativeSystemBalance,
            String systemBalanceStatus       // "POSITIVE" | "NEGATIVE"
    ) {}

    public record RoomsBlock(
            long totalGames,
            long botWins,
            long realPlayerWins,
            double botWinRatePercent,
            double avgRealPlayersPerRoom,
            double avgBotFillRatePercent
    ) {}

    public record PlayersBlock(
            long uniqueWinners,
            double boostUsageRatePercent,
            double winnerBoostRatePercent
    ) {}

    public record AnalyticsSummaryResponse(
            Instant from,
            Instant to,
            EconomicsBlock economics,
            RoomsBlock rooms,
            PlayersBlock players
    ) {
        public static AnalyticsSummaryResponse from(GameAnalyticsSummary s) {
            String balanceStatus = s.cumulativeSystemBalance().compareTo(BigDecimal.ZERO) >= 0
                    ? "POSITIVE" : "NEGATIVE";
            return new AnalyticsSummaryResponse(
                    s.from(), s.to(),
                    new EconomicsBlock(
                            s.totalRealRevenue(), s.totalPrizesAwarded(), s.totalBoostRevenue(),
                            s.totalRetained(), s.retentionRatePercent(),
                            s.cumulativeSystemBalance(), balanceStatus),
                    new RoomsBlock(
                            s.totalGames(), s.botWins(), s.realPlayerWins(),
                            s.botWinRatePercent(), s.avgRealPlayersPerRoom(), s.avgBotFillRate()),
                    new PlayersBlock(
                            s.uniqueWinners(), s.boostUsageRatePercent(), s.winnerBoostRatePercent())
            );
        }
    }

    public record AnalyticsDashboardResponse(
            AnalyticsSummaryResponse summary,
            List<com.vsrna.game.application.analytics.TimeSeriesPoint> timeseries
    ) {}

    public record AdminGameRecord(
            java.util.UUID gameRoomId,
            Instant completedAt,
            java.util.UUID winnerUserId,
            boolean winnerIsBot,
            BigDecimal prizeAwarded,
            BigDecimal systemRevenue,
            String winCriteria,
            int realPlayersCount,
            int botCount,
            int boostUsedCount,
            boolean winnerUsedBoost
    ) {}

    public record AdminGamesResponse(
            List<AdminGameRecord> games,
            int totalCount
    ) {}
}
