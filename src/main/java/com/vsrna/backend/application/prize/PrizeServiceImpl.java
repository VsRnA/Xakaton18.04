package com.vsrna.backend.application.prize;

import com.vsrna.backend.domain.gameroom.*;
import com.vsrna.backend.domain.participant.*;
import com.vsrna.backend.domain.round.*;
import com.vsrna.backend.domain.history.*;
import com.vsrna.backend.application.balance.UserBalanceService;
import com.vsrna.backend.infrastructure.websocket.GameWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrizeServiceImpl implements PrizeService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final UserBalanceService userBalanceService;
    private final GameWebSocketPublisher wsPublisher;

    @Override
    @Transactional
    public void distributePrize(UUID roomId) {
        log.info("Distributing prize for room {}", roomId);

        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        RoundResult round2 = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, 2));
        List<ParticipantRoundEntry> entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(round2.getId()));

        // Победитель — rank=1 в раунде 2
        ParticipantRoundEntry winnerEntry = entries.stream()
                .filter(e -> e.getRankInRound() != null && e.getRankInRound() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No winner found for room " + roomId));

        GameParticipant winner = participantRepository.get(
                GameParticipantQuery.byId(winnerEntry.getParticipantId()));

        // Финалисты — списываем резерв
        List<GameParticipant> finalists = participantRepository.list(
                GameParticipantQuery.byRoomAndStatus(roomId, ParticipantStatus.FINALIST));
        for (GameParticipant p : finalists) {
            if (!p.isBot() && p.getUserId() != null) {
                userBalanceService.deductReserved(p.getUserId(), p.getReservedPoints(), roomId);
            }
        }

        BigDecimal prizePool = room.getPrizePoolAmount();
        BigDecimal prizeAwarded;
        BigDecimal systemRevenue;
        UUID winnerUserId = winner.isBot() ? null : winner.getUserId();

        if (!winner.isBot() && winner.getUserId() != null) {
            BigDecimal winnerShare = config.getWinnerPayoutPercentage()
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            prizeAwarded = prizePool.multiply(winnerShare).setScale(2, RoundingMode.HALF_UP);
            systemRevenue = prizePool.subtract(prizeAwarded);
            userBalanceService.creditPoints(winner.getUserId(), prizeAwarded, roomId);
        } else {
            prizeAwarded = BigDecimal.ZERO;
            systemRevenue = prizePool;
        }

        String winCriteria = determineWinCriteria(entries);

        GameHistory history = new GameHistory(roomId, winnerUserId, winner.isBot(),
                prizeAwarded, systemRevenue, winCriteria);
        gameHistoryRepository.create(history);

        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.finished(
                java.time.Instant.now()));

        wsPublisher.publishGameEvent(roomId, Map.of(
                "type", "GAME_FINISHED",
                "winnerParticipantId", winner.getId().toString(),
                "winnerIsBot", winner.isBot(),
                "prizeAwarded", prizeAwarded,
                "systemRevenue", systemRevenue,
                "winCriteria", winCriteria
        ));

        log.info("Room {} finished. Winner: {}, prize: {}", roomId, winner.getId(), prizeAwarded);
    }

    private String determineWinCriteria(List<ParticipantRoundEntry> entries) {
        if (entries.size() < 2) return "SCORE";
        entries.sort(java.util.Comparator.comparingInt(e -> e.getRankInRound() != null ? e.getRankInRound() : 99));
        ParticipantRoundEntry first = entries.get(0);
        ParticipantRoundEntry second = entries.get(1);
        if (first.getTotalScore() != null && second.getTotalScore() != null
                && first.getTotalScore().compareTo(second.getTotalScore()) != 0) {
            return "SCORE";
        }
        if (first.getSelectionCount() != second.getSelectionCount()) {
            return "SELECTION_COUNT_TIEBREAK";
        }
        return "TIMESTAMP_TIEBREAK";
    }
}
