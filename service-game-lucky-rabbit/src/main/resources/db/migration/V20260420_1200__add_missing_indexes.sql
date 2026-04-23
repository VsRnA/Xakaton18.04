-- Индекс на userId участников — используется в фильтрации по пользователю
CREATE INDEX IF NOT EXISTS idx_participants_user_id
    ON game."gameParticipants" ("userId");

-- Индекс на status участников — используется в фильтрации активных/дисквалифицированных
CREATE INDEX IF NOT EXISTS idx_participants_status
    ON game."gameParticipants" (status);

-- Индекс на status раундов — используется в поиске активного раунда
CREATE INDEX IF NOT EXISTS idx_round_results_status
    ON game."roundResults" (status);

-- Индекс на participantId записей раунда — основной join при подсчёте результатов
CREATE INDEX IF NOT EXISTS idx_round_entries_participant_id
    ON game."participantRoundEntries" ("participantId");

-- Индекс на roundResultId записей раунда — основной join при загрузке результатов раунда
CREATE INDEX IF NOT EXISTS idx_round_entries_round_result_id
    ON game."participantRoundEntries" ("roundResultId");

-- Составной индекс на gameRoomId + roundNumber бочек — join при загрузке бочек раунда
CREATE INDEX IF NOT EXISTS idx_barrels_room_round
    ON game.barrels ("gameRoomId", "roundNumber");

-- Составной индекс для join конфигурации комнаты по entryFeeAmount — используется при поиске комнат по стоимости
CREATE INDEX IF NOT EXISTS idx_config_room_entry_fee
    ON game."gameRoomConfig" ("gameRoomId", "entryFeeAmount");

-- Индекс на winnerUserId истории игр — используется в аналитических запросах
CREATE INDEX IF NOT EXISTS idx_game_history_winner_user_id
    ON game."gameHistory" ("winnerUserId");
