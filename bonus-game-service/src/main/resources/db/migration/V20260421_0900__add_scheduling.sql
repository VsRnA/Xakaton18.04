-- Add SCHEDULED status support and room scheduling fields

ALTER TABLE "gameRoomConfig"
    ADD COLUMN "scheduledStartAt" TIMESTAMPTZ NULL,
    ADD COLUMN "repeatInterval"   VARCHAR(20)  NULL;
