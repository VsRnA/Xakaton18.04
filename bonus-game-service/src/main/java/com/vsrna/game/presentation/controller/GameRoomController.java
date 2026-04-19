package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.gameroom.CreateGameRoomCommand;
import com.vsrna.game.application.gameroom.GameRoomDetails;
import com.vsrna.game.application.gameroom.GameRoomService;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.presentation.dto.gameroom.GameRoomDto;
import com.vsrna.game.presentation.filter.AuthTokenFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/game/rooms")
@RequiredArgsConstructor
@Tag(name = "Game Rooms", description = "Управление игровыми комнатами")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @Operation(
            summary = "Создать комнату (ADMIN)",
            description = """
                    **WS-события после вызова:**
                    | Топик | Событие |
                    |-------|---------|
                    | `/topic/rooms` | `ROOM_CREATED` |
                    """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomDto.GameRoomResponse createRoom(
            @Valid @RequestBody GameRoomDto.CreateGameRoomRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        CreateGameRoomCommand command = new CreateGameRoomCommand(
                userId,
                request.maxPlayers(),
                request.entryFeeAmount(),
                request.winnerPayoutPercentage(),
                request.boostCostAmount(),
                request.boostEnabled(),
                request.maxBarrelSelection()
        );
        return toResponse(gameRoomService.createRoom(command));
    }

    @Operation(summary = "Список комнат")
    @GetMapping
    public List<GameRoomDto.GameRoomResponse> listRooms(
            @RequestParam(required = false) GameRoomStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return gameRoomService.listRooms(status, page, size).stream()
                .map(this::toResponse)
                .toList();
    }

    @Operation(summary = "Получить комнату")
    @GetMapping("/{roomId}")
    public GameRoomDto.GameRoomResponse getRoom(@PathVariable UUID roomId,
                                                HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return toResponse(gameRoomService.getRoom(roomId));
    }

    @Operation(
            summary = "Войти в комнату",
            description = """
                    **WS-события после вызова:**
                    | Топик | Событие | Условие |
                    |-------|---------|---------|
                    | `/topic/room/{roomId}` | `ROOM_UPDATED` | всегда |
                    | `/topic/rooms` | `ROOM_FULL` | комната заполнена сразу |
                    | `/topic/room/{roomId}/round` | `ROUND_STARTED` | комната заполнена → раунд 1 стартует |

                    Если вы — первый игрок, сервер ждёт таймаут заполнения. По истечении придут
                    `/topic/rooms` `ROOM_STARTED` и `/topic/room/{roomId}/round` `ROUND_STARTED` асинхронно.
                    """
    )
    @PostMapping("/{roomId}/join")
    public GameRoomDto.JoinRoomResponse joinRoom(@PathVariable UUID roomId,
                                                  HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        GameRoomDetails details = gameRoomService.joinRoom(roomId, userId);
        return new GameRoomDto.JoinRoomResponse(
                userId,
                details.config().getEntryFeeAmount(),
                details.room().getCurrentPlayerCount(),
                details.room().getPrizePoolAmount()
        );
    }

    private GameRoomDto.GameRoomResponse toResponse(GameRoomDetails details) {
        return new GameRoomDto.GameRoomResponse(
                details.room().getId(),
                details.room().getStatus(),
                details.room().getCurrentPlayerCount(),
                details.room().getPrizePoolAmount(),
                details.room().getCreatedAt(),
                new GameRoomDto.ConfigResponse(
                        details.config().getMaxPlayers(),
                        details.config().getEntryFeeAmount(),
                        details.config().getWinnerPayoutPercentage(),
                        details.config().getBoostCostAmount(),
                        details.config().isBoostEnabled(),
                        details.config().getMaxBarrelSelection()
                )
        );
    }

    private UUID requireAuth(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized("bearer token required");
        }
        return userId;
    }
}
