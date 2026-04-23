package com.prodforge.game.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

public final class GameErrorMessages {

    private GameErrorMessages() {}

    // === Auth ===
    public static final String AUTH_BEARER_REQUIRED = "bearer token required";
    public static final String AUTH_TOKEN_INVALID = "invalid or expired token";
    public static final String AUTH_ADMIN_REQUIRED = "access denied: admin role required";

    // === Game Room ===
    public static final String ROOM_NOT_ACCEPTING = "Room is not accepting players";
    public static final String ROOM_FULL = "Room is full";
    public static final String ROOM_PARTICIPANT_ALREADY_JOINED = "User already joined this room";
    public static final String ROOM_ONLY_WAITING_CAN_CANCEL = "Only WAITING rooms can be cancelled";
    public static final String ROOM_NO_SUITABLE_FOUND = "No suitable WAITING room found for the given parameters";
    public static final String ROOM_CONFIG_HAS_ERRORS =
            "Room config has errors. Set confirmWarnings=true to force creation.";

    public static String insufficientBalanceForEntry(BigDecimal required) {
        return "Insufficient bonus points to join the room. Required: " + required;
    }

    // === Boost ===
    public static final String BOOST_NOT_ENABLED = "Boost is not enabled in this room";
    public static final String BOOST_WRONG_ROUND_STATUS = "Boost can only be purchased during the active round";
    public static final String BOOST_ALREADY_USED = "Boost already used in round 1 — only one boost allowed per game";
    public static final String BOOST_ALREADY_PURCHASED_THIS_ROUND = "Boost already purchased in this round";

    public static String insufficientBalanceForBoost(BigDecimal required) {
        return "Insufficient points to purchase boost. Required: " + required;
    }

    // === Selection ===
    public static String selectionOutOfRange(int max) {
        return "Select between 1 and " + max + " barrels";
    }

    public static String roundNotInProgress(int roundNumber) {
        return "Round " + roundNumber + " is not in progress";
    }

    public static String barrelNotInRound(UUID barrelId) {
        return "Barrel " + barrelId + " does not belong to this round";
    }

    // === Rate Limiting ===
    public static final String JOIN_RATE_LIMITED = "Too many join attempts. Please wait before trying again.";

    // === Prize ===
    public static String noWinnerFound(UUID roomId) {
        return "No winner found for room " + roomId;
    }
}
