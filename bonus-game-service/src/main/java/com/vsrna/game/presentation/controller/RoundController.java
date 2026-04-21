package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.round.GameHistoryDetails;
import com.vsrna.game.application.round.RoundResultDetails;
import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.exception.GameErrorMessages;
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
                    Покупает буст в течение 30-секундного раунда (`ROUND_1` / `ROUND_2`), пока веса бочек ещё не раскрыты.
                    После окончания раунда наступает фаза `BOOST_DECISION` (5 сек ожидания без покупки),
                    затем `BOOST_WINDOW` — раскрытие весов и применение эффекта буста.
                    Буст применяется автоматически: меняет знак наибольшей отрицательной бочки среди выбранных,
                    либо удваивает минимальную положительную, если отрицательных нет.
                    Эффект буста отображается в WS-событии `BOOST_WINDOW_STARTED` в поле `boostEffects`.
                    Можно купить только один раз за игру. Списание баланса происходит после коммита транзакции.
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
                    | Топик | Событие | Описание |
                    |-------|---------|----------|
                    | `/topic/room/{roomId}/round` | `BOOST_DECISION_STARTED` | Ожидание 5 сек, веса не раскрыты, покупка буста недоступна |
                    | `/topic/room/{roomId}/round` | `BOOST_WINDOW_STARTED` | Раскрытие весов (`barrelWeights`) и эффект буста (`boostEffects`), 5 сек |
                    | `/topic/room/{roomId}/round` | `ROUND_COMPLETED` | Итоги раунда |
                    | `/topic/room/{roomId}/game` | `FINALISTS_ANNOUNCED` (конец раунда 1) или `GAME_FINISHED` (конец раунда 2) | |
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

    @Operation(summary = "История игры в комнате",
            description = "Возвращает детальную историю: состав участников, бусты, скоры, ранги, победитель.")
    @GetMapping("/history")
    public RoundDto.GameHistoryDetailResponse getGameHistory(@PathVariable UUID roomId,
                                                              HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        GameHistoryDetails details = roundService.getGameHistoryDetails(roomId);
        GameHistory h = details.history();
        List<RoundDto.ParticipantHistoryEntry> participants = details.participants().stream()
                .map(p -> new RoundDto.ParticipantHistoryEntry(
                        p.participantId(), p.userId(), p.isBot(), p.displayName(),
                        p.boostPurchased(), p.totalScore(), p.rank(), p.isWinner()))
                .toList();
        return new RoundDto.GameHistoryDetailResponse(
                h.getGameRoomId(), h.getWinnerUserId(), h.isWinnerIsBot(),
                h.getPrizeAwarded(), h.getSystemRevenue(), h.getCompletedAt(),
                h.getWinCriteria(), h.getRealPlayersCount(), h.getBotCount(),
                h.isWinnerUsedBoost(), participants
        );
    }

    private UUID requireAuth(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized(GameErrorMessages.AUTH_BEARER_REQUIRED);
        }
        return userId;
    }
}
