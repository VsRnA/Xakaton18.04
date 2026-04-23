package com.prodforge.backend.presentation.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

public sealed interface WsRoomsEvent permits
        WsRoomsEvent.RoomCreated,
        WsRoomsEvent.RoomScheduled,
        WsRoomsEvent.RoomFull,
        WsRoomsEvent.RoomStarted,
        WsRoomsEvent.RoomFinished,
        WsRoomsEvent.RoomCancelled {

    @Schema(description = "Создана новая игровая комната (немедленный старт)")
    record RoomCreated(
            @Schema(example = "ROOM_CREATED") String type,
            @Schema(description = "ID новой комнаты") String roomId
    ) implements WsRoomsEvent {}

    @Schema(description = "Создана комната по расписанию — откроется в scheduledStartAt")
    record RoomScheduled(
            @Schema(example = "ROOM_SCHEDULED") String type,
            @Schema(description = "ID комнаты") String roomId
    ) implements WsRoomsEvent {}

    @Schema(description = "Комната заполнена — игра начинается немедленно")
    record RoomFull(
            @Schema(example = "ROOM_FULL") String type,
            @Schema(description = "ID комнаты") String roomId
    ) implements WsRoomsEvent {}

    @Schema(description = "Комната запущена (после ожидания / заполнения ботами)")
    record RoomStarted(
            @Schema(example = "ROOM_STARTED") String type,
            @Schema(description = "ID комнаты") String roomId
    ) implements WsRoomsEvent {}

    @Schema(description = "Игра в комнате завершена — победитель определён")
    record RoomFinished(
            @Schema(example = "ROOM_FINISHED") String type,
            @Schema(description = "ID комнаты") String roomId,
            @Schema(description = "true если победил бот") boolean winnerIsBot,
            @Schema(description = "Сумма приза в бонусных баллах", example = "900.00") String prizeAwarded
    ) implements WsRoomsEvent {}

    @Schema(description = "Комната отменена администратором — ставки возвращены участникам асинхронно")
    record RoomCancelled(
            @Schema(example = "ROOM_CANCELLED") String type,
            @Schema(description = "ID отменённой комнаты") String roomId
    ) implements WsRoomsEvent {}
}
