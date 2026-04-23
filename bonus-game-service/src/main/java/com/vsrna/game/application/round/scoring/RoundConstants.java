package com.vsrna.game.application.round.scoring;

import java.math.BigDecimal;

public final class RoundConstants {

    private RoundConstants() {}

    public static final int ROUND_1 = 1;
    public static final int ROUND_2 = 2;

    public static final BigDecimal BOOST_MULTIPLIER = new BigDecimal("1.5");
    public static final int BARRELS_PER_ROUND = 12;
    public static final int FINALISTS_COUNT = 2;
}
