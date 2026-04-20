-- Optimistic locking: version columns for concurrent update detection

ALTER TABLE "gameRooms"
    ADD COLUMN IF NOT EXISTS "version" BIGINT NOT NULL DEFAULT 0;

ALTER TABLE "roundResults"
    ADD COLUMN IF NOT EXISTS "version" BIGINT NOT NULL DEFAULT 0;
