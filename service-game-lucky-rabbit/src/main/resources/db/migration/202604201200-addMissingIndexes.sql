CREATE INDEX IF NOT EXISTS idx_participants_user_id
    ON game."gameParticipants" ("userId");

CREATE INDEX IF NOT EXISTS idx_participants_status
    ON game."gameParticipants" (status);

CREATE INDEX IF NOT EXISTS idx_round_results_status
    ON game."roundResults" (status);

CREATE INDEX IF NOT EXISTS idx_round_entries_participant_id
    ON game."participantRoundEntries" ("participantId");

CREATE INDEX IF NOT EXISTS idx_round_entries_round_result_id
    ON game."participantRoundEntries" ("roundResultId");

CREATE INDEX IF NOT EXISTS idx_barrels_room_round
    ON game.barrels ("gameRoomId", "roundNumber");

CREATE INDEX IF NOT EXISTS idx_config_room_entry_fee
    ON game."gameRoomConfig" ("gameRoomId", "entryFeeAmount");

CREATE INDEX IF NOT EXISTS idx_game_history_winner_user_id
    ON game."gameHistory" ("winnerUserId");
