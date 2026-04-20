package com.vsrna.backend.presentation.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

public sealed interface WsRoomEvent permits WsRoomEvent.RoomUpdated {

    @Schema(description = "Изменилось состояние комнаты (новый игрок зашёл, изменился призовой фонд)")
    record RoomUpdated(
            @Schema(example = "ROOM_UPDATED") String type,
            @Schema(description = "Текущее количество игроков") int currentPlayers,
            @Schema(description = "Размер призового фонда", example = "400.00") String prizePool,
            @Schema(description = "Вероятность выигрыша (1/N)", example = "0.25") double winProbability,
            @Schema(description = "Unix-миллисекунды окончания таймера ожидания лобби. Null если таймер не запущен или уже истёк", example = "1713520800000", nullable = true) Long waitExpiresAt
    ) implements WsRoomEvent {}
}
