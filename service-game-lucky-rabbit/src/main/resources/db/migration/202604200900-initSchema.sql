CREATE SCHEMA IF NOT EXISTS game;

CREATE TABLE IF NOT EXISTS game."gameRooms" (
    id                   UUID         NOT NULL,
    status               VARCHAR(255) NOT NULL,
    "createdByUserId"    UUID         NOT NULL,
    "createdAt"          TIMESTAMP    NOT NULL,
    "startedAt"          TIMESTAMP,
    "finishedAt"         TIMESTAMP,
    "waitTimerExpiresAt" TIMESTAMP,
    "currentPlayerCount" INT          NOT NULL,
    "prizePoolAmount"    NUMERIC(12,2) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS game."gameRoomConfig" (
    "gameRoomId"              UUID          NOT NULL,
    "maxPlayers"              INT           NOT NULL,
    "entryFeeAmount"          NUMERIC(12,2) NOT NULL,
    "winnerPayoutPercentage"  NUMERIC(5,2)  NOT NULL,
    "boostCostAmount"         NUMERIC(12,2) NOT NULL,
    "isBoostEnabled"          BOOLEAN       NOT NULL,
    "maxBarrelSelection"      INT           NOT NULL,
    PRIMARY KEY ("gameRoomId")
);

CREATE TABLE IF NOT EXISTS game.barrels (
    id             UUID         NOT NULL,
    "gameRoomId"   UUID         NOT NULL,
    "roundNumber"  INT          NOT NULL,
    "barrelCode"   VARCHAR(255) NOT NULL,
    "displayOrder" INT          NOT NULL,
    weight         NUMERIC(5,2),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS game."gameParticipants" (
    id               UUID          NOT NULL,
    "gameRoomId"     UUID          NOT NULL,
    "userId"         UUID,
    "isBot"          BOOLEAN       NOT NULL,
    "displayName"    VARCHAR(100),
    "reservedPoints" NUMERIC(12,2) NOT NULL,
    status           VARCHAR(255)  NOT NULL,
    "advancedToFinal" BOOLEAN      NOT NULL,
    "joinedAt"       TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_participant_room_user UNIQUE ("gameRoomId", "userId")
);

CREATE TABLE IF NOT EXISTS game."roundResults" (
    id             UUID         NOT NULL,
    "gameRoomId"   UUID         NOT NULL,
    "roundNumber"  INT          NOT NULL,
    status         VARCHAR(255) NOT NULL,
    "seedHash"     VARCHAR(255),
    "rawSeed"      VARCHAR(255),
    "startedAt"    TIMESTAMP    NOT NULL,
    "endedAt"      TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS game."participantRoundEntries" (
    id                   UUID          NOT NULL,
    "roundResultId"      UUID          NOT NULL,
    "participantId"      UUID          NOT NULL,
    "boostPurchased"     BOOLEAN       NOT NULL DEFAULT FALSE,
    "boostedBarrelId"    UUID,
    "totalScore"         NUMERIC(8,2),
    "selectionTimestamp" TIMESTAMP,
    "selectionCount"     INT           NOT NULL DEFAULT 0,
    "rankInRound"        INT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS game."participantBarrelSelections" (
    "entryId"  UUID NOT NULL,
    "barrelId" UUID NOT NULL,
    PRIMARY KEY ("entryId", "barrelId")
);

CREATE TABLE IF NOT EXISTS game."gameHistory" (
    id                   UUID          NOT NULL,
    "gameRoomId"         UUID          NOT NULL,
    "winnerUserId"       UUID,
    "winnerIsBot"        BOOLEAN       NOT NULL,
    "prizeAwarded"       NUMERIC(12,2),
    "systemRevenue"      NUMERIC(12,2),
    "completedAt"        TIMESTAMP     NOT NULL,
    "winCriteria"        VARCHAR(255),
    "summaryJson"        TEXT,
    "realPlayersCount"   INT           NOT NULL DEFAULT 0,
    "botCount"           INT           NOT NULL DEFAULT 0,
    "realPlayersRevenue" NUMERIC(12,2),
    "boostRevenue"       NUMERIC(12,2),
    "boostUsedCount"     INT           NOT NULL DEFAULT 0,
    "winnerUsedBoost"    BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uq_game_history_room UNIQUE ("gameRoomId")
);

CREATE TABLE IF NOT EXISTS game.outbox_events (
    id             UUID         NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    topic          VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    processed_at   TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON game.outbox_events (status);
