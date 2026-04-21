package com.vsrna.game.domain.exception;

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
            "Конфигурация содержит ошибки. Установите confirmWarnings=true для принудительного создания.";

    public static String insufficientBalanceForEntry(BigDecimal required) {
        return "Недостаточно бонусных баллов для входа в комнату. Требуется: " + required;
    }

    // === Boost ===
    public static final String BOOST_NOT_ENABLED = "Boost is not enabled in this room";
    public static final String BOOST_WRONG_ROUND_STATUS = "Boost can only be purchased during the active round";
    public static final String BOOST_ALREADY_USED = "Boost already used in round 1 — only one boost allowed per game";

    public static String insufficientBalanceForBoost(BigDecimal required) {
        return "Недостаточно баллов для покупки буста. Требуется: " + required;
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

    // === Prize ===
    public static String noWinnerFound(UUID roomId) {
        return "No winner found for room " + roomId;
    }
}
