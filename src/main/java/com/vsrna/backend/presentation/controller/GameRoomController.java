package com.vsrna.backend.presentation.controller;

import com.vsrna.backend.application.gameroom.CreateGameRoomCommand;
import com.vsrna.backend.application.gameroom.GameRoomService;
import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.gameroom.GameRoom;
import com.vsrna.backend.domain.gameroom.GameRoomConfig;
import com.vsrna.backend.domain.gameroom.GameRoomConfigQuery;
import com.vsrna.backend.domain.gameroom.GameRoomConfigRepository;
import com.vsrna.backend.domain.gameroom.GameRoomStatus;
import com.vsrna.backend.presentation.dto.gameroom.GameRoomDto;
import com.vsrna.backend.presentation.filter.AuthTokenFilter;
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
    private final GameRoomConfigRepository gameRoomConfigRepository;

    @Operation(summary = "Создать комнату (ADMIN)")
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
                request.boostEnabled()
        );
        GameRoom room = gameRoomService.createRoom(command);
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(room.getId()));
        return toResponse(room, config);
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
                .map(room -> {
                    GameRoomConfig cfg = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(room.getId()));
                    return toResponse(room, cfg);
                })
                .toList();
    }

    @Operation(summary = "Получить комнату")
    @GetMapping("/{roomId}")
    public GameRoomDto.GameRoomResponse getRoom(@PathVariable UUID roomId,
                                                HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        GameRoom room = gameRoomService.getRoom(roomId);
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        return toResponse(room, config);
    }

    @Operation(summary = "Войти в комнату")
    @PostMapping("/{roomId}/join")
    public GameRoomDto.JoinRoomResponse joinRoom(@PathVariable UUID roomId,
                                                  HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        GameRoom room = gameRoomService.joinRoom(roomId, userId);
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        return new GameRoomDto.JoinRoomResponse(
                userId, // participantId placeholder — actual found in participants
                config.getEntryFeeAmount(),
                room.getCurrentPlayerCount(),
                room.getPrizePoolAmount()
        );
    }

    private GameRoomDto.GameRoomResponse toResponse(GameRoom room, GameRoomConfig config) {
        return new GameRoomDto.GameRoomResponse(
                room.getId(),
                room.getStatus(),
                room.getCurrentPlayerCount(),
                room.getPrizePoolAmount(),
                room.getCreatedAt(),
                new GameRoomDto.ConfigResponse(
                        config.getMaxPlayers(),
                        config.getEntryFeeAmount(),
                        config.getWinnerPayoutPercentage(),
                        config.getBoostCostAmount(),
                        config.isBoostEnabled()
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
