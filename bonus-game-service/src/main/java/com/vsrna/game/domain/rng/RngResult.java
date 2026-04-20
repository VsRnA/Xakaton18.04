package com.vsrna.game.domain.rng;

import java.math.BigDecimal;
import java.util.List;

public record RngResult(List<BigDecimal> weights, String seedHex, String seedHash) {}
