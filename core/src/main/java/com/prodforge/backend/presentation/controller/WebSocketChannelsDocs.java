package com.prodforge.backend.presentation.controller;

import com.prodforge.backend.presentation.dto.ws.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws/docs")
@Tag(
        name = "WebSocket Channels",
        description = """
                **Подключение (SockJS + STOMP):**
                ```
                const sock = new SockJS('/ws/game');
                const client = Stomp.over(sock);
                client.connect({'Authorization': 'Bearer <token>'}, () => {
                    client.subscribe('/topic/rooms', msg => { ... });
                });
                ```
                Все методы ниже — **документация** (возвращают 501). \
                Поле `type` в каждом событии является дискриминатором."""
)
public class WebSocketChannelsDocs {

    @GetMapping("/channels/rooms")
    @Operation(
            summary = "SUBSCRIBE /topic/rooms",
            description = """
                    Глобальный топик — широковещательные события по всем комнатам.
                    Подпишитесь при загрузке лобби.

                    **STOMP:** `client.subscribe('/topic/rooms', handler)`

                    Возможные события: `ROOM_CREATED`, `ROOM_SCHEDULED`, `ROOM_FULL`, `ROOM_STARTED`, `ROOM_FINISHED`, `ROOM_CANCELLED`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Схема сообщения топика",
            content = @Content(schema = @Schema(oneOf = {
                    WsRoomsEvent.RoomCreated.class,
                    WsRoomsEvent.RoomScheduled.class,
                    WsRoomsEvent.RoomFull.class,
                    WsRoomsEvent.RoomStarted.class,
                    WsRoomsEvent.RoomFinished.class,
                    WsRoomsEvent.RoomCancelled.class
            }))
    )
    public ResponseEntity<Void> subscribeRooms() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/channels/room/{roomId}")
    @Operation(
            summary = "SUBSCRIBE /topic/room/{roomId}",
            description = """
                    Событие конкретной комнаты. Подпишитесь сразу после входа в комнату.

                    **STOMP:** `client.subscribe('/topic/room/' + roomId, handler)`

                    Возможные события: `ROOM_UPDATED`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Схема сообщения топика",
            content = @Content(schema = @Schema(oneOf = {
                    WsRoomEvent.RoomUpdated.class
            }))
    )
    public ResponseEntity<Void> subscribeRoom() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/channels/room/{roomId}/round")
    @Operation(
            summary = "SUBSCRIBE /topic/room/{roomId}/round",
            description = """
                    События раундов комнаты. Подпишитесь после входа.

                    **STOMP:** `client.subscribe('/topic/room/' + roomId + '/round', handler)`

                    Возможные события:
                    - `ROUND_STARTED` — начало раунда, переданы ровно 12 ID бочек + `seedHash` для верификации
                    - `PLAYER_SELECTED` — прогресс выборов игроков
                    - `BOOST_DECISION_STARTED` — раунд завершён, веса ещё не раскрыты, 5 сек до раскрытия
                    - `BOOST_WINDOW_STARTED` — веса раскрыты (`barrelWeights`, `rawSeed`), показан эффект буста (`boostEffects`), 5 сек
                    - `ROUND_COMPLETED` — раунд завершён, объявлен победитель; содержит `disqualifiedIds`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Схема сообщения топика",
            content = @Content(schema = @Schema(oneOf = {
                    WsRoundEvent.RoundStarted.class,
                    WsRoundEvent.PlayerSelected.class,
                    WsRoundEvent.BoostDecisionStarted.class,
                    WsRoundEvent.BoostWindowStarted.class,
                    WsRoundEvent.RoundCompleted.class
            }))
    )
    public ResponseEntity<Void> subscribeRound() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/channels/room/{roomId}/game")
    @Operation(
            summary = "SUBSCRIBE /topic/room/{roomId}/game",
            description = """
                    Ключевые игровые события комнаты. Подпишитесь после входа.

                    **STOMP:** `client.subscribe('/topic/room/' + roomId + '/game', handler)`

                    Возможные события:
                    - `FINALISTS_ANNOUNCED` — топ-2 раунда 1 переходят в финал
                    - `GAME_FINISHED` — игра завершена, приз распределён
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Схема сообщения топика",
            content = @Content(schema = @Schema(oneOf = {
                    WsGameEvent.FinalistsAnnounced.class,
                    WsGameEvent.GameFinished.class
            }))
    )
    public ResponseEntity<Void> subscribeGame() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/channels/user/game/{roomId}")
    @Operation(
            summary = "SUBSCRIBE /user/queue/game/{roomId}",
            description = """
                    Персональный топик — бочки в уникальном порядке для конкретного игрока.

                    **STOMP:** `client.subscribe('/user/queue/game/' + roomId, handler)`

                    > Требует аутентификации. Сервер отправляет сообщение только вам.

                    Возможные события: `BARRELS_DEALT`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Схема сообщения топика",
            content = @Content(schema = @Schema(oneOf = {
                    WsUserEvent.BarrelsDealt.class
            }))
    )
    public ResponseEntity<Void> subscribeUserBarrels() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/channels/user/balance")
    @Operation(
            summary = "SUBSCRIBE /user/queue/balance",
            description = """
                    Персональный топик — обновления баланса пользователя.

                    **STOMP:** `client.subscribe('/user/queue/balance', handler)`

                    > Требует аутентификации. Сервер отправляет сообщение только вам.

                    Возможные события: `BALANCE_UPDATED`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Схема сообщения топика",
            content = @Content(schema = @Schema(oneOf = {
                    WsUserEvent.BalanceUpdated.class
            }))
    )
    public ResponseEntity<Void> subscribeUserBalance() {
        return ResponseEntity.notFound().build();
    }
}
