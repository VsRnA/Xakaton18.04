package com.vsrna.game.presentation.dto.gameevent;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public class GameEventDto {

    public record GameEventResponse(
            UUID id,
            UUID roomId,
            @Schema(description = "Тип события: ROOM_CREATED, ROOM_SCHEDULED, PLAYER_JOINED, ROOM_STARTED, " +
                    "ROUND_STARTED, ROUND_COMPLETED, GAME_FINISHED, ROOM_CANCELLED")
            String eventType,
            @Schema(description = "Детали события (ключ=значение)", nullable = true)
            String details,
            Instant occurredAt
    ) {}
}
