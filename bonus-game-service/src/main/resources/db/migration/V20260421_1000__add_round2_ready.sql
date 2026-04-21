-- Add round2Ready flag to track finalist readiness before Round 2 starts

ALTER TABLE "gameParticipants"
    ADD COLUMN "round2Ready" BOOLEAN NOT NULL DEFAULT FALSE;
