package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.gameroom.ConfigEvaluationResult;
import com.vsrna.game.application.gameroom.CreateGameRoomCommand;
import com.vsrna.game.application.gameroom.GameRoomDetails;
import com.vsrna.game.application.gameroom.GameRoomService;
import com.vsrna.game.application.gameroom.NextGameOption;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.GameRoomQuery;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Collection;

@RestController
@RequestMapping("/api/v1/game/rooms")
@RequiredArgsConstructor
@Tag(name = "Game Rooms", description = "Управление игровыми комнатами")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @Operation(summary = "Создать комнату (ADMIN)",
            description = """
                    Создаёт комнату и возвращает её данные вместе с предупреждениями о конфигурации.

                    **WS-события после вызова:**
                    | Топик | Событие |
                    |-------|---------|
                    | `/topic/rooms` | `ROOM_CREATED` |
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameRoomDto.CreateRoomResponse createRoom(
            @Valid @RequestBody GameRoomDto.CreateGameRoomRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = requireAdminAuth(httpRequest);
        CreateGameRoomCommand command = new CreateGameRoomCommand(
                userId,
                request.maxPlayers(),
                request.entryFeeAmount(),
                request.winnerPayoutPercentage(),
                request.boostCostAmount(),
                request.boostEnabled(),
                request.maxBarrelSelection()
        );
        ConfigEvaluationResult evaluation = gameRoomService.evaluateConfig(command);
        GameRoomDetails details = gameRoomService.createRoom(command);
        List<GameRoomDto.ConfigWarningResponse> warnings = evaluation.warnings().stream()
                .map(w -> new GameRoomDto.ConfigWarningResponse(w.code(), w.severity(), w.message()))
                .toList();
        return new GameRoomDto.CreateRoomResponse(toResponse(details), warnings);
    }

    @Operation(summary = "Список комнат с фильтрами",
            description = "Фильтрация по цене входа, числу мест, наличию свободных мест. При наличии фильтров сортировка по заполненности (убывание).")
    @GetMapping
    public List<GameRoomDto.GameRoomResponse> listRooms(
            @RequestParam(required = false) GameRoomStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BigDecimal entryFeeMin,
            @RequestParam(required = false) BigDecimal entryFeeMax,
            @RequestParam(required = false) Integer maxPlayers,
            @RequestParam(required = false) Boolean onlyWithSlots,
            HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return gameRoomService.listRooms(
                        GameRoomQuery.filtered(status, entryFeeMin, entryFeeMax, maxPlayers, onlyWithSlots, page, size))
                .stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Подобрать комнату",
            description = "Возвращает наиболее подходящую WAITING-комнату по целевой цене входа (±20%) и числу мест.")
    @GetMapping("/suggest")
    public GameRoomDto.GameRoomResponse suggestRoom(
            @RequestParam(required = false) BigDecimal entryFee,
            @RequestParam(required = false) Integer maxPlayers,
            HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return toResponse(gameRoomService.suggestRoom(entryFee, maxPlayers));
    }

    @Operation(summary = "Оценить конфигурацию комнаты (ADMIN)",
            description = "Возвращает финансовый анализ и предупреждения для заданной конфигурации без создания комнаты.")
    @PostMapping("/admin/evaluate")
    public GameRoomDto.ConfigEvaluationResponse evaluateConfig(
            @Valid @RequestBody GameRoomDto.CreateGameRoomRequest request,
            HttpServletRequest httpRequest) {
        requireAdminAuth(httpRequest);
        CreateGameRoomCommand command = new CreateGameRoomCommand(
                null, request.maxPlayers(), request.entryFeeAmount(),
                request.winnerPayoutPercentage(), request.boostCostAmount(),
                request.boostEnabled(), request.maxBarrelSelection()
        );
        ConfigEvaluationResult result = gameRoomService.evaluateConfig(command);
        List<GameRoomDto.ConfigWarningResponse> warnings = result.warnings().stream()
                .map(w -> new GameRoomDto.ConfigWarningResponse(w.code(), w.severity(), w.message()))
                .toList();
        return new GameRoomDto.ConfigEvaluationResponse(
                result.projectedPrizePool(),
                result.projectedSystemRevenue(),
                result.systemRevenuePercent(),
                result.playerExpectedValue(),
                result.attractivenessScore(),
                warnings
        );
    }

    @Operation(summary = "Отменить комнату (ADMIN)",
            description = "Отменяет WAITING-комнату и возвращает зарезервированные баллы участникам.")
    @DeleteMapping("/admin/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelRoom(@PathVariable UUID roomId, HttpServletRequest httpRequest) {
        UUID adminUserId = requireAdminAuth(httpRequest);
        gameRoomService.cancelRoom(roomId, adminUserId);
    }

    @Operation(summary = "Получить комнату")
    @GetMapping("/{roomId}")
    public GameRoomDto.GameRoomResponse getRoom(@PathVariable UUID roomId,
                                                HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return toResponse(gameRoomService.getRoom(roomId));
    }

    @Operation(summary = "Войти в комнату",
            description = """
                    **WS-события после вызова:**
                    | Топик | Событие | Условие |
                    |-------|---------|---------|
                    | `/topic/room/{roomId}` | `ROOM_UPDATED` | всегда |
                    | `/topic/rooms` | `ROOM_FULL` | комната заполнена сразу |
                    | `/topic/room/{roomId}/round` | `ROUND_STARTED` | комната заполнена → раунд 1 стартует |

                    При недостатке баллов: HTTP 402 с полем `details.suggestedRooms` — список более дешёвых комнат.
                    """)
    @PostMapping("/{roomId}/join")
    public GameRoomDto.JoinRoomResponse joinRoom(@PathVariable UUID roomId,
                                                 HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        String displayName = (String) httpRequest.getAttribute(AuthTokenFilter.USERNAME_ATTR);
        GameRoomDetails details = gameRoomService.joinRoom(roomId, userId, displayName);
        Instant waitTimerExpiresAt = details.room().getWaitTimerExpiresAt();
        return new GameRoomDto.JoinRoomResponse(
                userId,
                details.config().getEntryFeeAmount(),
                details.room().getCurrentPlayerCount(),
                details.room().getPrizePoolAmount(),
                waitTimerExpiresAt != null ? waitTimerExpiresAt.toEpochMilli() : null
        );
    }

    @Operation(summary = "Список участников комнаты",
            description = "Возвращает участников с вероятностью победы (на основе текущего числа игроков).")
    @GetMapping("/{roomId}/participants")
    public List<GameRoomDto.ParticipantResponse> listParticipants(@PathVariable UUID roomId,
                                                                   HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        int totalPlayers = gameRoomService.getRoom(roomId).room().getCurrentPlayerCount();
        double probPct = totalPlayers > 0 ? 100.0 / totalPlayers : 100.0;
        return gameRoomService.listParticipants(roomId).stream()
                .map(p -> new GameRoomDto.ParticipantResponse(
                        p.getId(),
                        p.getDisplayName(),
                        p.isBot(),
                        p.getStatus(),
                        probPct
                ))
                .toList();
    }

    @Operation(summary = "Следующая игра",
            description = "Возвращает до 3 рекомендаций после завершения игры: SAME (аналогичная), SAFER (дешевле), RISKIER (дороже).")
    @GetMapping("/{roomId}/next-game")
    public List<GameRoomDto.NextGameOption> nextGame(@PathVariable UUID roomId,
                                                     HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        List<NextGameOption> options = gameRoomService.nextGame(roomId);
        return options.stream()
                .map(o -> new GameRoomDto.NextGameOption(o.type(), toResponse(o.room())))
                .toList();
    }

    private GameRoomDto.GameRoomResponse toResponse(GameRoomDetails details) {
        Instant waitTimerExpiresAt = details.room().getWaitTimerExpiresAt();
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
                ),
                waitTimerExpiresAt != null ? waitTimerExpiresAt.toEpochMilli() : null
        );
    }

    private UUID requireAuth(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized("bearer token required");
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    private UUID requireAdminAuth(HttpServletRequest request) {
        UUID userId = requireAuth(request);
        Collection<String> roles = (Collection<String>) request.getAttribute(AuthTokenFilter.ROLES_ATTR);
        if (roles == null || !roles.contains("admin")) {
            throw ApiException.forbidden("access denied: admin role required");
        }
        return userId;
    }
}
