package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.round.RoundResultDetails;
import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.presentation.dto.round.RoundDto;
import com.vsrna.game.presentation.filter.AuthTokenFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/game/rooms/{roomId}")
@RequiredArgsConstructor
@Tag(name = "Round", description = "Управление раундами")
public class RoundController {

    private final RoundService roundService;

    @Operation(summary = "Получить перемешанные бочки раунда")
    @GetMapping("/rounds/{n}/barrels")
    public List<RoundDto.BarrelResponse> getBarrels(@PathVariable UUID roomId,
                                                     @PathVariable int n,
                                                     HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        return roundService.getShuffledBarrels(roomId, userId, n).stream()
                .map(b -> new RoundDto.BarrelResponse(b.getId(), b.getBarrelCode(), b.getWeight()))
                .toList();
    }

    @Operation(
            summary = "Купить буст",
            description = """
                    Покупает право выбросить одну бочку во время буст-окна (5 сек после `WEIGHTS_REVEALED`).
                    Списание баланса происходит после коммита транзакции.

                    **WS-события:** нет прямых. Эффект виден в `WEIGHTS_REVEALED` (буст-окно уже открыто)
                    и в финальном счёте раунда.
                    """
    )
    @PostMapping("/rounds/{n}/boost")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purchaseBoost(@PathVariable UUID roomId,
                              @PathVariable int n,
                              HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.purchaseBoost(roomId, userId, n);
    }

    @Operation(
            summary = "Выбрать бочки",
            description = """
                    Сохраняет выбор 1–5 бочек для текущего раунда. Можно вызывать повторно — выбор перезаписывается.

                    **WS-события после вызова:**
                    | Топик | Событие | Условие |
                    |-------|---------|---------|
                    | `/topic/room/{roomId}/round` | `PLAYER_SELECTED` | всегда |

                    **Асинхронные события по таймеру раунда (30 сек от старта):**
                    | Топик | Событие |
                    |-------|---------|
                    | `/topic/room/{roomId}/round` | `WEIGHTS_REVEALED` → буст-окно 5 сек |
                    | `/topic/room/{roomId}/round` | `ROUND_COMPLETED` |
                    | `/topic/room/{roomId}/game` | `FINALISTS_ANNOUNCED` (конец раунда 1) или `GAME_FINISHED` (конец раунда 2) |
                    """
    )
    @PostMapping("/rounds/{n}/selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitSelection(@PathVariable UUID roomId,
                                @PathVariable int n,
                                @Valid @RequestBody RoundDto.SubmitSelectionRequest request,
                                HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.submitSelection(roomId, userId, n, request.barrelIds(), Instant.now());
    }

    @Operation(
            summary = "Выбросить бочку (boost discard)",
            description = """
                    Применяет купленный буст: исключает одну бочку из итогового счёта.
                    Доступно только в течение буст-окна (5 сек после `WEIGHTS_REVEALED`).

                    **WS-события:** нет прямых. Результат отразится в `ROUND_COMPLETED`.
                    """
    )
    @PostMapping("/rounds/{n}/discard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardBarrel(@PathVariable UUID roomId,
                              @PathVariable int n,
                              @Valid @RequestBody RoundDto.DiscardBarrelRequest request,
                              HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.applyBoostDiscard(roomId, userId, n, request.barrelId());
    }

    @Operation(summary = "Результат раунда")
    @GetMapping("/rounds/{n}/result")
    public RoundDto.RoundResultResponse getRoundResult(@PathVariable UUID roomId,
                                                        @PathVariable int n,
                                                        HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        RoundResultDetails details = roundService.getRoundResult(roomId, n);

        List<RoundDto.ParticipantScoreResponse> scores = details.scores().stream()
                .map(s -> new RoundDto.ParticipantScoreResponse(
                        s.participantId(), s.isBot(), s.totalScore(),
                        s.selectionCount(), s.rank()))
                .toList();

        return new RoundDto.RoundResultResponse(n, details.roundResult().getSeedHash(),
                details.roundResult().getRawSeed(), scores, details.winnerId());
    }

    @Operation(summary = "История игры в комнате")
    @GetMapping("/history")
    public RoundDto.GameHistoryResponse getGameHistory(@PathVariable UUID roomId,
                                                        HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        GameHistory history = roundService.getGameHistory(roomId);
        return new RoundDto.GameHistoryResponse(
                history.getGameRoomId(),
                history.getWinnerUserId(),
                history.isWinnerIsBot(),
                history.getPrizeAwarded(),
                history.getSystemRevenue(),
                history.getCompletedAt(),
                history.getWinCriteria()
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
