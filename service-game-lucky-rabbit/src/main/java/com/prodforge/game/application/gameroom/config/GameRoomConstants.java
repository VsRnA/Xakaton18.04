package com.prodforge.game.application.gameroom.config;

import java.math.BigDecimal;

public final class GameRoomConstants {

    private GameRoomConstants() {}

    // Barrel naming: R1B01 .. R2B12
    public static final String BARREL_NAME_FORMAT = "R%dB%02d";

    // Fee range for suggest / nextGame
    public static final BigDecimal FEE_RANGE_MIN_FACTOR   = new BigDecimal("0.8");  // lower bound ±20%
    public static final BigDecimal FEE_RANGE_MAX_FACTOR   = new BigDecimal("1.2");  // upper bound ±20%
    public static final BigDecimal SAME_FEE_MIN_FACTOR    = new BigDecimal("0.9");  // "same" game lower bound
    public static final BigDecimal SAME_FEE_MAX_FACTOR    = new BigDecimal("1.1");  // "same" game upper bound
    public static final BigDecimal SAFER_FEE_DIVISOR      = new BigDecimal("2");    // safer = entryFee / 2
    public static final BigDecimal RISKIER_FEE_MIN_FACTOR = new BigDecimal("1.5"); // riskier = entryFee * 1.5

    // Next game option types
    public static final String NEXT_GAME_SAME    = "SAME";
    public static final String NEXT_GAME_SAFER   = "SAFER";
    public static final String NEXT_GAME_RISKIER = "RISKIER";

    // Config validator thresholds
    public static final BigDecimal MIN_PAYOUT_PERCENT     = new BigDecimal("50");
    public static final BigDecimal MAX_PAYOUT_PERCENT     = new BigDecimal("95");
    public static final BigDecimal HIGH_PAYOUT_THRESHOLD  = new BigDecimal("70");
    public static final BigDecimal BOOST_AFFORDABILITY_HALF = new BigDecimal("2"); // boost cost <= entryFee / 2
    public static final BigDecimal PERCENT_SCALE          = new BigDecimal("100");
    public static final int        PERCENT_DIVISION_SCALE = 4;
}
