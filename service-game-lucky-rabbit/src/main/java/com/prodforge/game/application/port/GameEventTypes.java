package com.prodforge.game.application.port;

public final class GameEventTypes {

    private GameEventTypes() {}

    public static final String FIELD_TYPE = "type";

    // Room events
    public static final String ROOM_SCHEDULED = "ROOM_SCHEDULED";
    public static final String ROOM_CREATED   = "ROOM_CREATED";
    public static final String ROOM_UPDATED   = "ROOM_UPDATED";
    public static final String ROOM_STARTED   = "ROOM_STARTED";
    public static final String ROOM_CANCELLED = "ROOM_CANCELLED";

    // Round events
    public static final String ROUND_STARTED          = "ROUND_STARTED";
    public static final String BOOST_DECISION_STARTED = "BOOST_DECISION_STARTED";
    public static final String BOOST_WINDOW_STARTED   = "BOOST_WINDOW_STARTED";
    public static final String ROUND_COMPLETED        = "ROUND_COMPLETED";
    public static final String FINALISTS_ANNOUNCED    = "FINALISTS_ANNOUNCED";

    // Audit log events
    public static final String PLAYER_JOINED      = "PLAYER_JOINED";
    public static final String WEIGHTS_REVEALED   = "WEIGHTS_REVEALED";
    public static final String PARTICIPANT_SCORED = "PARTICIPANT_SCORED";
}
