-- Add entryFeeAmount and boostAvailable to gameHistory
-- entryFeeAmount — стоимость входа в игру (сохраняется из конфига комнаты)
-- boostAvailable — был ли буст доступен в этой игре

ALTER TABLE game."gameHistory"
    ADD COLUMN IF NOT EXISTS "entryFeeAmount" NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS "boostAvailable" BOOLEAN NOT NULL DEFAULT FALSE;
