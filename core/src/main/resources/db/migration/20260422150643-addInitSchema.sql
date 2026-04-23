CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE IF NOT EXISTS core.users (
    guid              UUID         NOT NULL,
    phone             VARCHAR(20)  NOT NULL,
    username          VARCHAR(100),
    password          VARCHAR(255) NOT NULL,
    name              VARCHAR(100),
    "lastName"        VARCHAR(100),
    "patronymicName"  VARCHAR(100),
    "createdAt"       TIMESTAMP    NOT NULL,
    "updatedAt"       TIMESTAMP    NOT NULL,
    PRIMARY KEY (guid),
    CONSTRAINT uq_users_phone    UNIQUE (phone),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS core."userRoles" (
    guid    UUID        NOT NULL,
    keyword VARCHAR(50) NOT NULL,
    name    VARCHAR(100) NOT NULL,
    PRIMARY KEY (guid),
    CONSTRAINT uq_user_roles_keyword UNIQUE (keyword)
);

CREATE TABLE IF NOT EXISTS core."userRoleAssigment" (
    "userGuid" UUID NOT NULL,
    "roleGuid" UUID NOT NULL,
    PRIMARY KEY ("userGuid", "roleGuid"),
    CONSTRAINT fk_role_assignment_user FOREIGN KEY ("userGuid") REFERENCES core.users (guid),
    CONSTRAINT fk_role_assignment_role FOREIGN KEY ("roleGuid") REFERENCES core."userRoles" (guid)
);

CREATE TABLE IF NOT EXISTS core."userBalances" (
    "userId"    UUID          NOT NULL,
    available   NUMERIC(12,2) NOT NULL,
    reserved    NUMERIC(12,2) NOT NULL,
    version     BIGINT        NOT NULL,
    "updatedAt" TIMESTAMP     NOT NULL,
    PRIMARY KEY ("userId")
);

CREATE TABLE IF NOT EXISTS core."pointTransactions" (
    id               UUID          NOT NULL,
    "userId"         UUID          NOT NULL,
    type             VARCHAR(255)  NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    "gameRoomId"     UUID,
    description      VARCHAR(255),
    "idempotencyKey" VARCHAR(255),
    "createdAt"      TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_point_transactions_idempotency UNIQUE ("idempotencyKey")
);
