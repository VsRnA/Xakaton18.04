package com.vsrna.game.application.prize;

import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.round.RoundScoringUtils;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.*;
import com.vsrna.game.domain.history.*;
import com.vsrna.game.domain.participant.*;
import com.vsrna.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final BalancePort balancePort;
    private final GameEventPort gameEventPort;
    private final GameNotifierPort notifierPort;

    @Override
    @Transactional
    public void distributePrize(UUID roomId) {
        log.info("Distributing prize for room {}", roomId);

        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));

        RoundResult round2 = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, 2));
        List<ParticipantRoundEntry> entries = new ArrayList<>(entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(round2.getId())));

        ParticipantRoundEntry winnerEntry = entries.stream()
                .filter(entry -> entry.getRankInRound() != null && entry.getRankInRound() == 1)
                .findFirst()
                .orElseThrow(() -> ApiException.internal("No winner found for room " + roomId));

        GameParticipant winner = participantRepository.get(
                GameParticipantQuery.byId(winnerEntry.getParticipantId()));

        List<GameParticipant> finalists = participantRepository.list(
                GameParticipantQuery.byRoomAndStatus(roomId, ParticipantStatus.FINALIST));

        BigDecimal prizePool = room.getPrizePoolAmount();
        BigDecimal prizeAwarded;
        BigDecimal systemRevenue;
        UUID winnerUserId = winner.isBot() ? null : winner.getUserId();

        if (winner.isRealPlayer()) {
            BigDecimal winnerShare = config.getWinnerPayoutPercentage()
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            prizeAwarded = prizePool.multiply(winnerShare).setScale(2, RoundingMode.HALF_UP);
            systemRevenue = prizePool.subtract(prizeAwarded);
        } else {
            prizeAwarded = BigDecimal.ZERO;
            systemRevenue = prizePool;
        }

        entries.sort(Comparator.comparingInt(e -> e.getRankInRound() != null ? e.getRankInRound() : 99));
        String winCriteria = RoundScoringUtils.determineWinCriteria(entries);

        List<GameParticipant> allParticipants = participantRepository.list(GameParticipantQuery.byRoom(roomId));
        int realPlayersCount = (int) allParticipants.stream().filter(participant -> !participant.isBot()).count();
        int botCount = (int) allParticipants.stream().filter(GameParticipant::isBot).count();
        BigDecimal realPlayersRevenue = config.getEntryFeeAmount()
                .multiply(BigDecimal.valueOf(realPlayersCount));

        RoundResult round1 = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, 1));
        List<ParticipantRoundEntry> round1Entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(round1.getId()));
        long boostCountR1 = round1Entries.stream().filter(ParticipantRoundEntry::isBoostPurchased).count();
        long boostCountR2 = entries.stream().filter(ParticipantRoundEntry::isBoostPurchased).count();
        int boostUsedCount = (int) (boostCountR1 + boostCountR2);
        BigDecimal boostRevenue = config.getBoostCostAmount().multiply(BigDecimal.valueOf(boostUsedCount));
        boolean winnerUsedBoost = winnerEntry.isBoostPurchased();

        GameHistory history = new GameHistory(roomId, winnerUserId, winner.isBot(),
                prizeAwarded, systemRevenue, winCriteria,
                realPlayersCount, botCount, realPlayersRevenue,
                boostRevenue, boostUsedCount, winnerUsedBoost);
        gameHistoryRepository.create(history);

        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.finished(Instant.now()));

        notifierPort.publishGameEvent(roomId, Map.of(
                "type", "GAME_FINISHED",
                "winnerParticipantId", winner.getId().toString(),
                "winnerIsBot", winner.isBot(),
                "prizeAwarded", prizeAwarded,
                "systemRevenue", systemRevenue,
                "winCriteria", winCriteria
        ));

        notifierPort.publishRoomsUpdate(Map.of(
                "type", "ROOM_FINISHED",
                "roomId", roomId.toString(),
                "winnerIsBot", winner.isBot(),
                "prizeAwarded", prizeAwarded
        ));

        gameEventPort.publishGameFinished(roomId, winnerUserId, winner.isBot(),
                prizePool, prizeAwarded, systemRevenue, winCriteria);

        log.info("Room {} finished. Winner: {}, prize: {}", roomId, winner.getId(), prizeAwarded);

        final GameParticipant finalWinner = winner;
        final BigDecimal finalPrizeAwarded = prizeAwarded;
        final List<GameParticipant> finalistsCopy = new ArrayList<>(finalists);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (GameParticipant participant : finalistsCopy) {
                    if (participant.isRealPlayer()) {
                        try {
                            balancePort.release(participant.getUserId(), participant.getReservedPoints(), roomId);
                        } catch (Exception e) {
                            log.error("COMPENSATION NEEDED: failed to release balance for finalist " +
                                      "userId={}, roomId={}: {}", participant.getUserId(), roomId, e.getMessage());
                        }
                    }
                }
                if (finalWinner.isRealPlayer()) {
                    try {
                        balancePort.award(finalWinner.getUserId(), finalPrizeAwarded, roomId);
                    } catch (Exception e) {
                        log.error("COMPENSATION NEEDED: failed to award prize " +
                                  "userId={}, roomId={}, amount={}: {}",
                                  finalWinner.getUserId(), roomId, finalPrizeAwarded, e.getMessage());
                    }
                }
            }
        });
    }
}
