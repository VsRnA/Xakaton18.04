package com.vsrna.game.domain.rng;

import java.util.UUID;

/**
 * Порт генерации случайных весов (Commit-Reveal).
 * Определён в domain — application зависит от абстракции, а не от реализации.
 */
public interface RngPort {
    RngResult generate(UUID roomId, int roundNumber, int count);
}
