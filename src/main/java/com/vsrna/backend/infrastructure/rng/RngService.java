package com.vsrna.backend.infrastructure.rng;

import java.util.UUID;

public interface RngService {
    /**
     * Генерирует {@code count} весов в диапазоне [-50.00, +50.00].
     * Commit-Reveal: seedHash публикуется ДО весов (в БД), rawSeed — вместе с весами.
     */
    RngResult generate(UUID roomId, int roundNumber, int count);
}
