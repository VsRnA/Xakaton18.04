ALTER TABLE "gameRoomConfig"
    ADD COLUMN "scheduledStartAt" TIMESTAMPTZ NULL,
    ADD COLUMN "repeatInterval"   VARCHAR(20)  NULL;
