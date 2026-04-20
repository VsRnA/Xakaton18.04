package com.vsrna.game.application.gameroom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class GameRoomConfigValidator {

    public ConfigEvaluationResult evaluate(
            int maxPlayers,
            BigDecimal entryFee,
            BigDecimal payoutPct,
            BigDecimal boostCost,
            boolean boostEnabled,
            int maxBarrelSelection) {

        List<ConfigWarning> warnings = new ArrayList<>();

        // --- Бизнес-правила ---

        if (payoutPct.compareTo(BigDecimal.valueOf(50)) < 0) {
            warnings.add(ConfigWarning.warn("LOW_PLAYER_PAYOUT",
                    "Игрок получает менее 50% от взносов — комната будет непривлекательна для пользователей"));
        }

        if (payoutPct.compareTo(BigDecimal.valueOf(95)) > 0) {
            warnings.add(ConfigWarning.warn("LOW_ORGANIZER_REVENUE",
                    "Организатор получает менее 5% — комната невыгодна для платформы"));
        }

        if (boostEnabled && boostCost.compareTo(entryFee) > 0) {
            warnings.add(ConfigWarning.warn("BOOST_TOO_EXPENSIVE",
                    "Стоимость буста (" + boostCost + ") превышает цену входа (" + entryFee + ") — никто не купит буст"));
        }

        if (!boostEnabled && boostCost.compareTo(BigDecimal.ZERO) > 0) {
            warnings.add(ConfigWarning.error("BOOST_CONFIG_INCONSISTENT",
                    "Буст отключён, но стоимость буста задана > 0 — противоречивая конфигурация"));
        }

        if (maxBarrelSelection == 1) {
            warnings.add(ConfigWarning.warn("LOW_SELECTION_CHOICE",
                    "Выбор только 1 бочки — игра становится полностью случайной, без стратегии"));
        }

        if (maxPlayers == 2) {
            warnings.add(ConfigWarning.info("SMALL_ROOM",
                    "Комната на 2 игрока — буст не даёт статистического преимущества"));
        }

        // --- Финансовые расчёты ---
        BigDecimal totalPool = entryFee.multiply(BigDecimal.valueOf(maxPlayers));
        BigDecimal prizePool = totalPool.multiply(payoutPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal systemRevenue = totalPool.subtract(prizePool);
        double systemRevenuePct = totalPool.compareTo(BigDecimal.ZERO) > 0
                ? systemRevenue.divide(totalPool, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        // Матожидание для одного игрока: prizePool / maxPlayers - entryFee
        double expectedValue = prizePool.divide(BigDecimal.valueOf(maxPlayers), 2, RoundingMode.HALF_UP)
                .subtract(entryFee).doubleValue();

        // --- Оценка привлекательности ---
        boolean hasError = warnings.stream().anyMatch(w -> "ERROR".equals(w.severity()));
        boolean isHighPayout = payoutPct.compareTo(BigDecimal.valueOf(70)) >= 0;
        boolean isBoostAffordable = !boostEnabled || boostCost.compareTo(entryFee.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) <= 0;

        String attractiveness;
        if (hasError) {
            attractiveness = "LOW";
        } else if (isHighPayout && isBoostAffordable && warnings.isEmpty()) {
            attractiveness = "HIGH";
        } else if (isHighPayout && isBoostAffordable) {
            attractiveness = "MEDIUM";
        } else {
            attractiveness = "LOW";
        }

        return new ConfigEvaluationResult(prizePool, systemRevenue, systemRevenuePct, expectedValue,
                attractiveness, warnings);
    }
}
