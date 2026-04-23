package com.vsrna.backend.application.user;

import java.math.BigDecimal;

public final class UserConstants {

    private UserConstants() {}

    public static final BigDecimal INITIAL_BALANCE      = BigDecimal.valueOf(1_000_000);
    public static final String     PHONE_ALREADY_TAKEN    = "phone already taken";
    public static final String     USERNAME_ALREADY_TAKEN = "username already taken";
}
