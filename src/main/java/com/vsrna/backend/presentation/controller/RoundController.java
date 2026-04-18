package com.vsrna.backend.presentation.controller;

import com.vsrna.backend.application.round.RoundService;
import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.history.GameHistory;
import com.vsrna.backend.domain.history.GameHistoryQuery;
import com.vsrna.backend.domain.history.GameHistoryRepository;
import com.vsrna.backend.domain.participant.GameParticipant;
import com.vsrna.backend.domain.participant.GameParticipantQuery;
import com.vsrna.backend.domain.participant.GameParticipantRepository;
import com.vsrna.backend.domain.round.ParticipantRoundEntry;
import com.vsrna.backend.domain.round.ParticipantRoundEntryQuery;
import com.vsrna.backend.domain.round.ParticipantRoundEntryRepository;
import com.vsrna.backend.domain.round.RoundResult;
import com.vsrna.backend.domain.round.RoundResultQuery;
import com.vsrna.backend.domain.round.RoundResultRepository;
import com.vsrna.backend.presentation.dto.round.RoundDto;
import com.vsrna.backend.presentation.filter.AuthTokenFilter;
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
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final GameParticipantRepository participantRepository;
    private final GameHistoryRepository gameHistoryRepository;

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

    @Operation(summary = "Купить буст")
    @PostMapping("/rounds/{n}/boost")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purchaseBoost(@PathVariable UUID roomId,
                              @PathVariable int n,
                              HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.purchaseBoost(roomId, userId, n);
    }

    @Operation(summary = "Выбрать бочки")
    @PostMapping("/rounds/{n}/selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitSelection(@PathVariable UUID roomId,
                                @PathVariable int n,
                                @Valid @RequestBody RoundDto.SubmitSelectionRequest request,
                                HttpServletRequest httpRequest) {
        UUID userId = requireAuth(httpRequest);
        roundService.submitSelection(roomId, userId, n, request.barrelIds(), Instant.now());
    }

    @Operation(summary = "Выбросить бочку (boost discard)")
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
        RoundResult roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, n));
        List<ParticipantRoundEntry> entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId()));

        List<RoundDto.ParticipantScoreResponse> scores = entries.stream()
                .map(e -> {
                    GameParticipant p = participantRepository.get(GameParticipantQuery.byId(e.getParticipantId()));
                    return new RoundDto.ParticipantScoreResponse(
                            e.getParticipantId(), p.isBot(), e.getTotalScore(),
                            e.getSelectionCount(), e.getRankInRound());
                })
                .toList();

        UUID winnerId = entries.stream()
                .filter(e -> e.getRankInRound() != null && e.getRankInRound() == 1)
                .map(ParticipantRoundEntry::getParticipantId)
                .findFirst().orElse(null);

        return new RoundDto.RoundResultResponse(n, roundResult.getSeedHash(),
                roundResult.getRawSeed(), scores, winnerId);
    }

    @Operation(summary = "История игры в комнате")
    @GetMapping("/history")
    public RoundDto.GameHistoryResponse getGameHistory(@PathVariable UUID roomId,
                                                        HttpServletRequest httpRequest) {
        requireAuth(httpRequest);
        GameHistory history = gameHistoryRepository.get(GameHistoryQuery.byRoom(roomId));
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
