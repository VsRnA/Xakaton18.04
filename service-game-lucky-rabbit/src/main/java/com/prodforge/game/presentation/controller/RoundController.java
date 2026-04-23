package com.prodforge.game.presentation.controller;

import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.round.history.GameHistoryDetails;
import com.prodforge.game.application.round.RoundResultDetails;
import com.prodforge.game.application.round.RoundService;
import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.exception.GameErrorMessages;
import com.prodforge.game.domain.history.GameHistory;
import com.prodforge.game.presentation.dto.gameevent.GameEventDto;
import com.prodforge.game.presentation.dto.round.RoundDto;
import com.prodforge.game.presentation.filter.AuthTokenFilter;
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
    private final GameEventLogService gameEventLogService;

    @Operation(
            summary = "Получить перемешанные бочки раунда",
            description = "Возвращает бочки раунда в порядке, перемешанном по userId + номеру раунда (Provably Fair shuffle). Веса равны null до завершения раунда."
    )
    @GetMapping("/rounds/{roundNumber}/barrels")
    public List<RoundDto.BarrelResponse> getBarrels(@PathVariable UUID roomId,
                                                     @PathVariable int roundNumber,
                                                     HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        return roundService.getShuffledBarrels(roomId, userId, roundNumber).stream()
                .map(barrel -> new RoundDto.BarrelResponse(barrel.getId(), barrel.getBarrelCode(), barrel.getWeight()))
                .toList();
    }

    @Operation(
            summary = "Купить буст",
            description = """
                    Покупает буст в течение 30-секундного раунда (`ROUND_1` / `ROUND_2`), пока веса бочек ещё не раскрыты.
                    После окончания раунда наступает фаза `BOOST_DECISION_STARTED` (5 сек, покупка недоступна),
                    затем `BOOST_WINDOW_STARTED` — раскрытие весов и эффекта буста.
                    Буст применяется автоматически: меняет знак наибольшей отрицательной бочки среди выбранных,
                    либо удваивает минимальную положительную, если отрицательных нет.
                    Эффект отображается в WS-событии `BOOST_WINDOW_STARTED` в поле `boostEffects`.
                    Можно купить только один раз за игру. Списание баланса выполняется асинхронно через Kafka.
                    """
    )
    @PostMapping("/rounds/{roundNumber}/boost")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purchaseBoost(@PathVariable UUID roomId,
                              @PathVariable int roundNumber,
                              HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.purchaseBoost(roomId, userId, roundNumber);
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
    @PostMapping("/rounds/{roundNumber}/selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitSelection(@PathVariable UUID roomId,
                                @PathVariable int roundNumber,
                                @Valid @RequestBody RoundDto.SubmitSelectionRequest request,
                                HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.submitSelection(roomId, userId, roundNumber, request.barrelIds(), Instant.now());
    }

    @Operation(summary = "Результат раунда")
    @GetMapping("/rounds/{roundNumber}/result")
    public RoundDto.RoundResultResponse getRoundResult(@PathVariable UUID roomId,
                                                        @PathVariable int roundNumber,
                                                        HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        RoundResultDetails details = roundService.getRoundResult(roomId, roundNumber);

        List<RoundDto.ParticipantScoreResponse> scores = details.scores().stream()
                .map(score -> new RoundDto.ParticipantScoreResponse(
                        score.participantId(), score.isBot(), score.totalScore(),
                        score.selectionCount(), score.rank()))
                .toList();

        return new RoundDto.RoundResultResponse(roundNumber, details.roundResult().getSeedHash(),
                details.roundResult().getRawSeed(), scores, details.winnerId());
    }

    @Operation(
            summary = "Подтвердить готовность к финальному раунду",
            description = """
                    Финалист подтверждает готовность к старту Round 2.
                    Round 2 стартует когда оба финалиста подтвердили готовность, либо после истечения серверного таймаута.
                    Пока оба не готовы, серверный таймер Round 2 не запущен.
                    """
    )
    @PostMapping("/rounds/2/ready")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReady(@PathVariable UUID roomId, HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.markFinalistReady(roomId, userId);
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
    @GetMapping("/rounds/{roundNumber}/verify")
    public ResponseEntity<RoundDto.VerifyRoundResponse> verifyRound(@PathVariable UUID roomId,
                                                                      @PathVariable int roundNumber,
                                                                      HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        RoundResultDetails details = roundService.getRoundResult(roomId, roundNumber);
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
        } catch (Exception ex) {
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
        GameHistory history = details.history();
        List<RoundDto.ParticipantHistoryEntry> participants = details.participants().stream()
                .map(participant -> new RoundDto.ParticipantHistoryEntry(
                        participant.participantId(), participant.userId(), participant.isBot(), participant.displayName(),
                        participant.boostPurchased(), participant.totalScore(), participant.rank(), participant.isWinner()))
                .toList();
        return new RoundDto.GameHistoryDetailResponse(
                history.getGameRoomId(), history.getWinnerUserId(), history.isWinnerIsBot(),
                history.getEntryFeeAmount(), history.getRealPlayersRevenue(), history.getPrizeAwarded(),
                history.getSystemBalance(), history.getCompletedAt(), history.getWinCriteria(),
                history.getRealPlayersCount(), history.getBotCount(),
                history.isBoostAvailable(), history.getBoostUsedCount(), history.getBoostRevenue(),
                participants
        );
    }

    @Operation(
            summary = "Лог событий игры",
            description = """
                    Возвращает хронологический список событий, произошедших в комнате:
                    `ROOM_CREATED`, `ROOM_SCHEDULED`, `PLAYER_JOINED`, `ROOM_STARTED`,
                    `ROUND_STARTED`, `ROUND_COMPLETED`, `GAME_FINISHED`, `ROOM_CANCELLED`.

                    Поле `details` содержит дополнительный контекст в формате `ключ=значение`.
                    """)
    @GetMapping("/events")
    public List<GameEventDto.GameEventResponse> getGameEvents(@PathVariable UUID roomId,
                                                               HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        return gameEventLogService.getEvents(roomId).stream()
                .map(event -> new GameEventDto.GameEventResponse(event.getId(), event.getRoomId(),
                        event.getEventType(), event.getDetails(), event.getOccurredAt()))
                .toList();
    }

    private UUID requireAuth(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized(GameErrorMessages.AUTH_BEARER_REQUIRED);
        }
        return userId;
    }
}
