-- Индексы для ускорения фильтрации и JOIN комнат по конфигурации
CREATE INDEX IF NOT EXISTS idx_game_rooms_status ON game."gameRooms" (status);
CREATE INDEX IF NOT EXISTS idx_config_entry_fee ON game."gameRoomConfig" ("entryFeeAmount");
CREATE INDEX IF NOT EXISTS idx_config_max_players ON game."gameRoomConfig" ("maxPlayers");
CREATE INDEX IF NOT EXISTS idx_game_history_completed_at ON game."gameHistory" ("completedAt");
CREATE INDEX IF NOT EXISTS idx_participants_game_room ON game."gameParticipants" ("gameRoomId");
CREATE INDEX IF NOT EXISTS idx_round_results_room ON game."roundResults" ("gameRoomId", "roundNumber");
