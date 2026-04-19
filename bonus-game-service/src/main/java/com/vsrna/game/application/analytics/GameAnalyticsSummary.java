package com.vsrna.game.application.analytics;

import java.math.BigDecimal;
import java.time.Instant;

public record GameAnalyticsSummary(

        // Период
        Instant from,
        Instant to,

        // --- Экономика (оборот баллов) ---
        long totalGames,
        BigDecimal totalRealRevenue,       // сколько баллов собрано с живых игроков
        BigDecimal totalPrizesAwarded,     // сколько выплачено победителям
        BigDecimal totalBoostRevenue,      // доход от бустов
        BigDecimal totalRetained,          // удержано системой = totalRealRevenue + totalBoostRevenue - totalPrizesAwarded
        double retentionRatePercent,       // % удержания от реального дохода
        BigDecimal cumulativeSystemBalance,// накопленный баланс системы по всем играм

        // --- Комнаты ---
        long botWins,
        long realPlayerWins,
        double botWinRatePercent,
        double avgRealPlayersPerRoom,
        double avgBotFillRate,             // средний % заполнения ботами

        // --- Игроки ---
        long uniqueWinners,                // уникальных победителей-людей
        double boostUsageRatePercent,      // % игр, где хоть один игрок купил буст

        // --- Эффективность буста ---
        double winnerBoostRatePercent      // % побед живых игроков, у которых был буст

) {}
