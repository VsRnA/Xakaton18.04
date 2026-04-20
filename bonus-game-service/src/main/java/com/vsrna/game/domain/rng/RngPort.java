package com.vsrna.game.domain.rng;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Порт генерации случайных весов (Commit-Reveal).
 * Определён в domain — application зависит от абстракции, а не от реализации.
 *
 * Схема честной игры:
 *   1. commit(roomId, round)  → seedHash публикуется игрокам ДО выборов
 *   2. [игроки делают выборы]
 *   3. reveal(seedHex, count) → rawSeed публикуется ПОСЛЕ выборов; игрок верифицирует SHA256(rawSeed)==seedHash
 */
public interface RngPort {
    /** Фаза 1: генерируем seed, возвращаем {seedHex, seedHash}. Весов ещё нет. */
    RngCommitment commit(UUID roomId, int roundNumber);

    /** Фаза 2: детерминированно генерируем веса из ранее сохранённого seedHex. */
    List<BigDecimal> reveal(String seedHex, int count);
}
