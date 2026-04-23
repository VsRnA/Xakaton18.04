package com.prodforge.game.application.round.scoring;

import com.prodforge.game.domain.round.ParticipantBarrelSelection;
import com.prodforge.game.domain.round.ParticipantRoundEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoundScoringUtils {

    private static final Logger log = LoggerFactory.getLogger(RoundScoringUtils.class);

    private RoundScoringUtils() {}

    public static final String WIN_CRITERIA_SCORE            = "SCORE";
    public static final String WIN_CRITERIA_DIRECT           = "DIRECT";
    public static final String WIN_CRITERIA_SELECTION_COUNT  = "SELECTION_COUNT_TIEBREAK";
    public static final String WIN_CRITERIA_TIMESTAMP        = "TIMESTAMP_TIEBREAK";

    public record BoostEffect(UUID barrelId, BigDecimal originalWeight, BigDecimal boostedWeight) {}

    /**
     * Вычисляет эффект буста для одного игрока.
     *
     * Приоритет применения буста:
     *   1. Если есть отрицательные веса — выбирает самый отрицательный и ИНВЕРТИРУЕТ его (weight → -weight).
     *      Пример: -8 → +8.
     *   2. Если отрицательных нет — выбирает наименьший положительный и УДВАИВАЕТ его (weight × 2).
     *      Пример: +3 → +6.
     *   3. Если все веса нулевые или список пуст — буст не даёт эффекта.
     *
     * Буст применяется строго к одной бочке — той, которая даёт максимальный прирост.
     */
    public static BoostEffect computeBoostEffect(List<ParticipantBarrelSelection> selections,
                                                  Map<UUID, BigDecimal> barrelWeights) {
        if (selections.isEmpty()) return null;

        ParticipantBarrelSelection mostNegativeTarget = null;
        BigDecimal mostNegativeWeight = null;

        ParticipantBarrelSelection minPositiveTarget = null;
        BigDecimal minPositiveWeight = null;

        for (ParticipantBarrelSelection selection : selections) {
            BigDecimal weight = barrelWeights.get(selection.getBarrelId());
            if (weight == null) continue;
            if (weight.signum() < 0) {
                // Ищем самый отрицательный вес — именно его выгоднее всего инвертировать
                if (mostNegativeWeight == null || weight.compareTo(mostNegativeWeight) < 0) {
                    mostNegativeWeight = weight;
                    mostNegativeTarget = selection;
                }
            } else if (weight.signum() > 0) {
                // Ищем наименьший положительный — удвоение даёт меньший абсолютный прирост,
                // но это запасной вариант при отсутствии отрицательных весов
                if (minPositiveWeight == null || weight.compareTo(minPositiveWeight) < 0) {
                    minPositiveWeight = weight;
                    minPositiveTarget = selection;
                }
            }
        }

        if (mostNegativeTarget != null) {
            return new BoostEffect(mostNegativeTarget.getBarrelId(), mostNegativeWeight, mostNegativeWeight.negate());
        }
        if (minPositiveTarget != null) {
            return new BoostEffect(minPositiveTarget.getBarrelId(), minPositiveWeight, minPositiveWeight.multiply(BigDecimal.TWO));
        }
        return null;
    }

    /**
     * Считает итоговый счёт игрока за раунд.
     *
     * Алгоритм: сумма весов всех выбранных бочек.
     * Если куплен буст — вес одной бочки (определённой computeBoostEffect) заменяется на улучшенный.
     * Остальные бочки учитываются по оригинальному весу без изменений.
     */
    public static BigDecimal calculateScore(ParticipantRoundEntry entry,
                                            List<ParticipantBarrelSelection> selections,
                                            Map<UUID, BigDecimal> barrelWeights) {
        BoostEffect boostEffect = entry.isBoostPurchased()
                ? computeBoostEffect(selections, barrelWeights) : null;

        BigDecimal score = BigDecimal.ZERO;
        for (ParticipantBarrelSelection selection : selections) {
            BigDecimal barrelWeight = barrelWeights.get(selection.getBarrelId());
            if (barrelWeight == null) {
                log.warn("No weight found for barrelId={}, skipping in score calculation", selection.getBarrelId());
                continue;
            }
            if (boostEffect != null && selection.getBarrelId().equals(boostEffect.barrelId())) {
                // Для забустированной бочки подставляем улучшенный вес
                score = score.add(boostEffect.boostedWeight());
            } else {
                score = score.add(barrelWeight);
            }
        }
        return score;
    }

    /**
     * Определяет критерий победы по уже отсортированному списку участников.
     *
     * Приоритет критериев (от сильного к слабому):
     *   1. SCORE              — у победителя счёт выше второго места.
     *   2. SELECTION_COUNT_TIEBREAK — счета равны, но количество выбранных бочек различается
     *                                 (меньше бочек = лучше, т.к. список отсортирован по selectionCount ASC).
     *   3. TIMESTAMP_TIEBREAK — всё равно; победил тот, кто выбрал бочки раньше.
     */
    public static String determineWinCriteria(List<ParticipantRoundEntry> sorted) {
        if (sorted.size() < 2) return WIN_CRITERIA_SCORE;
        ParticipantRoundEntry first = sorted.get(0);
        ParticipantRoundEntry second = sorted.get(1);
        if (first.getTotalScore() != null && second.getTotalScore() != null
                && first.getTotalScore().compareTo(second.getTotalScore()) != 0) {
            return WIN_CRITERIA_SCORE;
        }
        if (first.getSelectionCount() != second.getSelectionCount()) {
            return WIN_CRITERIA_SELECTION_COUNT;
        }
        return WIN_CRITERIA_TIMESTAMP;
    }
}
