CREATE TABLE IF NOT EXISTS game."gameEventLog" (
    id           UUID         NOT NULL,
    "roomId"     UUID         NOT NULL,
    "eventType"  VARCHAR(64)  NOT NULL,
    details      VARCHAR(500),
    "occurredAt" TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_game_event_log_room_id
    ON game."gameEventLog" ("roomId", "occurredAt");
