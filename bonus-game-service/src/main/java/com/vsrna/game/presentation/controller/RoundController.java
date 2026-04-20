package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.round.RoundResultDetails;
import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.presentation.dto.round.RoundDto;
import com.vsrna.game.presentation.filter.AuthTokenFilter;
import org.springframework.http.ResponseEntity;
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

    @Operation(
            summary = "Получить перемешанные бочки раунда",
            description = "Возвращает бочки раунда в порядке, перемешанном по userId + номеру раунда (Provably Fair shuffle). Веса равны null до завершения раунда."
    )
    @GetMapping("/rounds/{n}/barrels")
    public List<RoundDto.BarrelResponse> getBarrels(@PathVariable UUID roomId,
                                                     @PathVariable int n,
                                                     HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        return roundService.getShuffledBarrels(roomId, userId, n).stream()
                .map(barrel -> new RoundDto.BarrelResponse(barrel.getId(), barrel.getBarrelCode(), barrel.getWeight()))
                .toList();
    }

    @Operation(
            summary = "Купить буст",
            description = """
                    Покупает буст во время фазы принятия решения (5 сек после `WEIGHTS_REVEALED`).
                    Позволяет применить усиление к одной бочке в фазе `BOOST_WINDOW`.
                    Списание баланса происходит после коммита транзакции.

                    **WS-события после покупки (публикуются на `/topic/room/{roomId}/round`):**
                    | Топик | Событие |
                    |-------|---------|
                    | `/topic/room/{roomId}/round` | `BOOST_WINDOW_STARTED` — открылось 5-секундное окно для `apply-boost` |
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
                    Сохраняет выбор 1–N бочек для текущего раунда (N = `maxBarrelSelection` конфига комнаты, максимум 10). Можно вызывать повторно — выбор перезаписывается.

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
            summary = "Применить буст к бочке",
            description = """
                    Применяет купленный буст к выбранной бочке во время буст-окна (5 сек после `BOOST_WINDOW_STARTED`).
                    Эффект: отрицательный вес → знак меняется на положительный; положительный вес → умножается на 1.5.
                    Доступно только в фазе `BOOST_WINDOW`.

                    **WS-события:** нет прямых. Результат отразится в `ROUND_COMPLETED`.
                    """
    )
    @PostMapping("/rounds/{n}/apply-boost")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void applyBoost(@PathVariable UUID roomId,
                           @PathVariable int n,
                           @Valid @RequestBody RoundDto.BoostBarrelRequest request,
                           HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.applyBoost(roomId, userId, n, request.barrelId());
    }

    @Operation(summary = "Результат раунда")
    @GetMapping("/rounds/{n}/result")
    public RoundDto.RoundResultResponse getRoundResult(@PathVariable UUID roomId,
                                                        @PathVariable int n,
                                                        HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        RoundResultDetails details = roundService.getRoundResult(roomId, n);

        List<RoundDto.ParticipantScoreResponse> scores = details.scores().stream()
                .map(score -> new RoundDto.ParticipantScoreResponse(
                        score.participantId(), score.isBot(), score.totalScore(),
                        score.selectionCount(), score.rank()))
                .toList();

        return new RoundDto.RoundResultResponse(n, details.roundResult().getSeedHash(),
                details.roundResult().getRawSeed(), scores, details.winnerId());
    }

    @Operation(
            summary = "Верификация честности раунда (Provably Fair)",
            description = """
                    Позволяет игроку самостоятельно проверить, что результаты раунда не были подтасованы.

                    **Схема проверки (commit-reveal):**
                    1. В начале раунда (`ROUND_STARTED`) сервер публикует `seedHash = SHA256(rawSeed)`
                    2. После окончания выборов (`WEIGHTS_REVEALED`) сервер раскрывает `rawSeed`
                    3. Игрок проверяет: `SHA256(rawSeed) == seedHash` — это гарантирует,
                       что веса бочек были зафиксированы ДО выборов и не менялись

                    Этот эндпоинт возвращает оба значения и результат проверки.
                    """
    )
    @GetMapping("/rounds/{n}/verify")
    public ResponseEntity<RoundDto.VerifyRoundResponse> verifyRound(@PathVariable UUID roomId,
                                                                      @PathVariable int n,
                                                                      HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        RoundResultDetails details = roundService.getRoundResult(roomId, n);
        String seedHash = details.roundResult().getSeedHash();
        String rawSeed = details.roundResult().getRawSeed();
        boolean valid = seedHash != null && rawSeed != null
                && seedHash.equals(sha256Hex(rawSeed));
        return ResponseEntity.ok(new RoundDto.VerifyRoundResponse(seedHash, rawSeed, valid));
    }

    private String sha256Hex(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(java.util.HexFormat.of().parseHex(input));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
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
