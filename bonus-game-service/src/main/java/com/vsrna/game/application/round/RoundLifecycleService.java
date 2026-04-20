package com.vsrna.game.application.round;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.port.GameSchedulerPort;
import com.vsrna.game.application.prize.PrizeService;
import com.vsrna.game.domain.barrel.Barrel;
import com.vsrna.game.domain.barrel.BarrelQuery;
import com.vsrna.game.domain.barrel.BarrelRepository;
import com.vsrna.game.domain.gameroom.GameRoomPatch;
import com.vsrna.game.domain.gameroom.GameRoomQuery;
import com.vsrna.game.domain.gameroom.GameRoomRepository;
import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.domain.history.GameHistoryRepository;
import com.vsrna.game.domain.participant.GameParticipant;
import com.vsrna.game.domain.participant.GameParticipantPatch;
import com.vsrna.game.domain.participant.GameParticipantQuery;
import com.vsrna.game.domain.participant.GameParticipantRepository;
import com.vsrna.game.domain.participant.ParticipantStatus;
import com.vsrna.game.domain.rng.RngCommitment;
import com.vsrna.game.domain.rng.RngPort;
import com.vsrna.game.domain.round.ParticipantBarrelSelection;
import com.vsrna.game.domain.round.ParticipantBarrelSelectionRepository;
import com.vsrna.game.domain.round.ParticipantRoundEntry;
import com.vsrna.game.domain.round.ParticipantRoundEntryPatch;
import com.vsrna.game.domain.round.ParticipantRoundEntryQuery;
import com.vsrna.game.domain.round.ParticipantRoundEntryRepository;
import com.vsrna.game.domain.round.RoundResult;
import com.vsrna.game.domain.round.RoundResultPatch;
import com.vsrna.game.domain.round.RoundResultQuery;
import com.vsrna.game.domain.round.RoundResultRepository;
import com.vsrna.game.domain.round.RoundResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoundLifecycleService {

    private final GameRoomRepository gameRoomRepository;
    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final ParticipantBarrelSelectionRepository selectionRepository;
    private final RngPort rngPort;
    private final GameSchedulerPort schedulerPort;
    private final GameNotifierPort notifierPort;
    private final PrizeService prizeService;
    private final GameHistoryRepository gameHistoryRepository;
    private final BotService botService;
    private final BalanceCompensationHelper balanceCompensationHelper;

    @Transactional
    public void startRound(UUID roomId, int roundNumber) {
        GameRoomStatus newStatus = roundNumber == 1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                new GameRoomPatch(newStatus, null, null, Instant.now(), null, null));

        RngCommitment commitment = rngPort.commit(roomId, roundNumber);
        RoundResult roundResult = roundResultRepository.create(new RoundResult(roomId, roundNumber));
        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                RoundResultPatch.commit(commitment.seedHash(), commitment.rawSeed()));

        schedulerPort.scheduleRoundEnd(roomId, roundNumber);

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        notifierPort.publishRoundEvent(roomId, Map.of(
                "type", "ROUND_STARTED",
                "roundNumber", roundNumber,
                "barrelIds", barrels.stream().map(barrel -> barrel.getId().toString()).toList(),
                "seedHash", commitment.seedHash(),
                "expiresAt", Instant.now().plusSeconds(30).toEpochMilli()
        ));
    }

    @Transactional
    public void resolveRound(UUID roomId, int roundNumber) {
        log.info("Resolving round {} for room {}", roundNumber, roomId);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<BigDecimal> weights = rngPort.reveal(roundResult.getRawSeed(), RoundConstants.BARRELS_PER_ROUND);

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        for (int i = 0; i < barrels.size(); i++) {
            barrels.get(i).setWeight(weights.get(i));
        }
        barrelRepository.updateAll(BarrelQuery.byRoomAndRound(roomId, roundNumber), barrels);

        GameRoomStatus decisionStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_DECISION_1 : GameRoomStatus.BOOST_DECISION_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(decisionStatus));

        Map<String, Object> weightsPayload = new LinkedHashMap<>();
        weightsPayload.put("type", "WEIGHTS_REVEALED");
        weightsPayload.put("roundNumber", roundNumber);
        Map<String, Object> weightMap = new LinkedHashMap<>();
        for (Barrel barrel : barrels) weightMap.put(barrel.getId().toString(), barrel.getWeight());
        weightsPayload.put("barrelWeights", weightMap);
        weightsPayload.put("seedHash", roundResult.getSeedHash());
        weightsPayload.put("rawSeed", roundResult.getRawSeed());
        weightsPayload.put("phase", "BOOST_DECISION");
        weightsPayload.put("expiresAt", Instant.now().plusSeconds(5).toEpochMilli());
        notifierPort.publishRoundEvent(roomId, weightsPayload);

        schedulerPort.scheduleBoostDecisionEnd(roomId, roundNumber);
    }

    @Transactional
    public void startBoostWindow(UUID roomId, int roundNumber) {
        log.info("Starting boost window for room {} round {}", roomId, roundNumber);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        roundResultRepository.update(RoundResultQuery.byId(roundResult.getId()), RoundResultPatch.boostWindow());

        GameRoomStatus boostStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_WINDOW_1 : GameRoomStatus.BOOST_WINDOW_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(boostStatus));

        notifierPort.publishRoundEvent(roomId, Map.of(
                "type", "BOOST_WINDOW_STARTED",
                "roundNumber", roundNumber,
                "expiresAt", Instant.now().plusSeconds(5).toEpochMilli()
        ));

        schedulerPort.scheduleBoostWindowEnd(roomId, roundNumber);
    }

    @Transactional
    public void finalizeRound(UUID roomId, int roundNumber) {
        log.info("Finalizing round {} for room {}", roundNumber, roomId);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Map<UUID, BigDecimal> barrelWeights = new HashMap<>();
        for (Barrel barrel : barrels) barrelWeights.put(barrel.getId(), barrel.getWeight());

        boolean protectionMode = gameHistoryRepository.getCumulativeSystemBalance()
                .compareTo(BigDecimal.ZERO) < 0;
        botService.submitBotSelections(roomId, roundNumber, protectionMode, barrelWeights);

        List<ParticipantRoundEntry> entries = new ArrayList<>(entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId())));

        List<UUID> entryIds = entries.stream().map(ParticipantRoundEntry::getId).toList();
        Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry = selectionRepository
                .listByEntries(entryIds).stream()
                .collect(Collectors.groupingBy(ParticipantBarrelSelection::getEntryId));

        entries = addDefaultEntriesForAbsentParticipants(entries, roundResult.getId(), roomId, roundNumber);

        List<String> disqualifiedIds = new ArrayList<>();
        entries = eliminateNoSelectionParticipants(entries, selectionsByEntry, roomId, disqualifiedIds);

        scoreAndRankEntries(entries, selectionsByEntry, barrelWeights);
        String winCriteria = RoundScoringUtils.determineWinCriteria(entries);

        for (int i = 0; i < entries.size(); i++) {
            ParticipantRoundEntry entry = entries.get(i);
            entryRepository.update(
                    ParticipantRoundEntryQuery.byId(entry.getId()),
                    ParticipantRoundEntryPatch.rank(i + 1, entry.getTotalScore()));
        }

        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                new RoundResultPatch(RoundResultStatus.COMPLETED, null, null, Instant.now()));

        Map<String, Object> roundCompletedPayload = new LinkedHashMap<>();
        roundCompletedPayload.put("type", "ROUND_COMPLETED");
        roundCompletedPayload.put("roundNumber", roundNumber);
        roundCompletedPayload.put("winnerId", entries.isEmpty() ? "" : entries.get(0).getParticipantId().toString());
        roundCompletedPayload.put("winCriteria", winCriteria);
        roundCompletedPayload.put("disqualifiedIds", disqualifiedIds);
        notifierPort.publishRoundEvent(roomId, roundCompletedPayload);

        if (roundNumber == 1) {
            advanceToFinal(roomId, entries, winCriteria);
        } else {
            prizeService.distributePrize(roomId);
        }
    }

    private List<ParticipantRoundEntry> addDefaultEntriesForAbsentParticipants(
            List<ParticipantRoundEntry> entries, UUID roundResultId, UUID roomId, int roundNumber) {
        ParticipantStatus statusForRound = roundNumber == 1 ? ParticipantStatus.ACTIVE : ParticipantStatus.FINALIST;
        List<GameParticipant> allParticipants = participantRepository.list(
                GameParticipantQuery.byRoomAndStatus(roomId, statusForRound));

        Set<UUID> submittedParticipantIds = entries.stream()
                .map(ParticipantRoundEntry::getParticipantId)
                .collect(Collectors.toCollection(HashSet::new));

        List<ParticipantRoundEntry> result = new ArrayList<>(entries);
        for (GameParticipant participant : allParticipants) {
            if (!submittedParticipantIds.contains(participant.getId())) {
                ParticipantRoundEntry defaultEntry = new ParticipantRoundEntry(roundResultId, participant.getId());
                defaultEntry.setSelectionTimestamp(Instant.now());
                result.add(entryRepository.create(defaultEntry));
            }
        }
        return result;
    }

    private List<ParticipantRoundEntry> eliminateNoSelectionParticipants(
            List<ParticipantRoundEntry> entries,
            Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry,
            UUID roomId,
            List<String> disqualifiedIds) {
        List<GameParticipant> disqualifiedRealPlayers = new ArrayList<>();
        List<ParticipantRoundEntry> remaining = new ArrayList<>();

        for (ParticipantRoundEntry entry : entries) {
            if (selectionsByEntry.getOrDefault(entry.getId(), List.of()).isEmpty()) {
                GameParticipant disqualified = participantRepository.update(
                        GameParticipantQuery.byId(entry.getParticipantId()),
                        GameParticipantPatch.eliminate());
                disqualifiedIds.add(entry.getParticipantId().toString());
                if (disqualified.isRealPlayer()) {
                    disqualifiedRealPlayers.add(disqualified);
                }
            } else {
                remaining.add(entry);
            }
        }

        balanceCompensationHelper.scheduleRelease(disqualifiedRealPlayers, roomId);
        return remaining;
    }

    private void scoreAndRankEntries(List<ParticipantRoundEntry> entries,
                                     Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry,
                                     Map<UUID, BigDecimal> barrelWeights) {
        for (ParticipantRoundEntry entry : entries) {
            List<ParticipantBarrelSelection> sels = selectionsByEntry.getOrDefault(entry.getId(), List.of());
            entry.setTotalScore(RoundScoringUtils.calculateScore(entry, sels, barrelWeights));
        }

        entries.sort(java.util.Comparator
                .comparing(ParticipantRoundEntry::getTotalScore,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparingInt(ParticipantRoundEntry::getSelectionCount)
                .thenComparing(ParticipantRoundEntry::getSelectionTimestamp,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
    }

    private void advanceToFinal(UUID roomId, List<ParticipantRoundEntry> sortedEntries, String winCriteria) {
        List<String> finalistIds = new ArrayList<>();
        List<GameParticipant> eliminated = new ArrayList<>();

        for (int i = 0; i < sortedEntries.size(); i++) {
            GameParticipantPatch patch = i < RoundConstants.FINALISTS_COUNT
                    ? GameParticipantPatch.advanceToFinal()
                    : GameParticipantPatch.eliminate();
            GameParticipant p = participantRepository.update(
                    GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()), patch);

            if (i < RoundConstants.FINALISTS_COUNT) {
                finalistIds.add(sortedEntries.get(i).getParticipantId().toString());
            } else if (p.isRealPlayer()) {
                eliminated.add(p);
            }
        }

        notifierPort.publishGameEvent(roomId, Map.of(
                "type", "FINALISTS_ANNOUNCED",
                "finalistIds", finalistIds,
                "winCriteria", winCriteria
        ));

        startRound(roomId, 2);

        balanceCompensationHelper.scheduleRelease(eliminated, roomId);
    }
}
