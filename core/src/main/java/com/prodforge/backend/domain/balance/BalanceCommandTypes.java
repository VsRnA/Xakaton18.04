package com.prodforge.backend.domain.balance;

public final class BalanceCommandTypes {

    private BalanceCommandTypes() {}

    public static final String RESERVE         = "RESERVE";
    public static final String RELEASE         = "RELEASE";
    public static final String AWARD           = "AWARD";
    public static final String DEDUCT          = "DEDUCT";
    public static final String DEDUCT_RESERVED = "DEDUCT_RESERVED";
}
