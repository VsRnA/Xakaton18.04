package com.vsrna.game.domain.rng;

/**
 * Результат фазы commit: seedHash публикуется игрокам, rawSeed хранится в БД до фазы reveal.
 */
public record RngCommitment(String rawSeed, String seedHash) {}
