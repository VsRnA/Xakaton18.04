package com.vsrna.game.presentation.dto.gameevent;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class GameEventDto {

    public record GameEventResponse(
            UUID id,
            UUID roomId,
            @Schema(description = """
                    Тип события:
                    - `ROOM_CREATED` — комната создана (`entryFee`, `maxPlayers`)
                    - `ROOM_SCHEDULED` — комната запланирована (`scheduledAt`)
                    - `ROOM_CANCELLED` — комната отменена (`cancelledBy`)
                    - `PLAYER_JOINED` — игрок вошёл (`userId`)
                    - `ROOM_STARTED` — игра началась (`totalPlayers`)
                    - `ROUND_STARTED` — раунд стартовал (`round`)
                    - `WEIGHTS_REVEALED` — веса раскрыты (`round`, `weights`: {barrelId → weight})
                    - `BARREL_SELECTED` — игрок выбрал бочки (`userId`, `round`, `barrels`: [id,...], `count`)
                    - `BOOST_PURCHASED` — куплен буст (`userId`, `round`, `cost`)
                    - `PARTICIPANT_SCORED` — итог участника (`participantId`, `round`, `score`, `barrels`, `totalWeight`, опционально `boost`: {`barrelId`, `from`, `to`})
                    - `ROUND_COMPLETED` — раунд завершён (`round`, `winCriteria`)
                    - `FINALISTS_ANNOUNCED` — финалисты определены (`finalists`: [id,...], `criteria`)
                    - `GAME_FINISHED` — игра завершена (`winnerId`, `prize`, `criteria`)
                    """)
            String eventType,
            @Schema(description = "Структурированные детали события")
            Map<String, Object> details,
            Instant occurredAt
    ) {}
}
