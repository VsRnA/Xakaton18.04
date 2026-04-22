package com.vsrna.game.application.round;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.port.GameEventPort;
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
import com.vsrna.game.domain.round.ParticipantBarrelSelectionQuery;
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
import com.vsrna.game.domain.exception.ApiException;
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
    private final GameEventPort gameEventPort;

    @Transactional
    public void startRound(UUID roomId, int roundNumber) {
        if (roundNumber == RoundConstants.ROUND_1) {
            List<GameParticipant> active = participantRepository.list(
                    GameParticipantQuery.byRoomAndStatus(roomId, ParticipantStatus.ACTIVE));
            if (active.size() <= RoundConstants.FINALISTS_COUNT) {
                bypassRound1(roomId, active);
                return;
            }
        }

        GameRoomStatus newStatus = roundNumber == RoundConstants.ROUND_1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
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

        GameRoomStatus decisionStatus = roundNumber == RoundConstants.ROUND_1
                ? GameRoomStatus.BOOST_DECISION_1 : GameRoomStatus.BOOST_DECISION_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(decisionStatus));

        notifierPort.publishRoundEvent(roomId, Map.of(
                "type", "BOOST_DECISION_STARTED",
                "roundNumber", roundNumber,
                "expiresAt", Instant.now().plusSeconds(5).toEpochMilli()
        ));

        schedulerPort.scheduleBoostDecisionEnd(roomId, roundNumber);
    }

    @Transactional
    public void startBoostWindow(UUID roomId, int roundNumber) {
        log.info("Starting boost window for room {} round {}", roomId, roundNumber);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        roundResultRepository.update(RoundResultQuery.byId(roundResult.getId()), RoundResultPatch.boostWindow());

        GameRoomStatus boostStatus = roundNumber == RoundConstants.ROUND_1
                ? GameRoomStatus.BOOST_WINDOW_1 : GameRoomStatus.BOOST_WINDOW_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(boostStatus));

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Map<UUID, BigDecimal> barrelWeights = buildBarrelWeightMap(barrels);

        Map<String, Object> payload = buildBoostWindowPayload(roomId, roundNumber, roundResult, barrels, barrelWeights);
        notifierPort.publishRoundEvent(roomId, payload);

        schedulerPort.scheduleBoostWindowEnd(roomId, roundNumber);
    }

    @Transactional
    public void finalizeRound(UUID roomId, int roundNumber) {
        log.info("Finalizing round {} for room {}", roundNumber, roomId);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Map<UUID, BigDecimal> barrelWeights = buildBarrelWeightMap(barrels);

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

        if (roundNumber == RoundConstants.ROUND_1) {
            advanceToFinal(roomId, entries, winCriteria);
        } else {
            prizeService.distributePrize(roomId);
        }
    }

    private List<ParticipantRoundEntry> addDefaultEntriesForAbsentParticipants(
            List<ParticipantRoundEntry> entries, UUID roundResultId, UUID roomId, int roundNumber) {
        ParticipantStatus statusForRound = roundNumber == RoundConstants.ROUND_1 ? ParticipantStatus.ACTIVE : ParticipantStatus.FINALIST;
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

        for (GameParticipant p : disqualifiedRealPlayers) {
            gameEventPort.publishBalanceDeductReserved(p.getUserId(), p.getReservedPoints(), roomId);
        }
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

    private Map<UUID, BigDecimal> buildBarrelWeightMap(List<Barrel> barrels) {
        Map<UUID, BigDecimal> weights = new HashMap<>(barrels.size() * 2);
        for (Barrel barrel : barrels) weights.put(barrel.getId(), barrel.getWeight());
        return weights;
    }

    private Map<String, Object> buildBoostWindowPayload(UUID roomId, int roundNumber,
                                                         RoundResult roundResult,
                                                         List<Barrel> barrels,
                                                         Map<UUID, BigDecimal> barrelWeights) {
        Map<UUID, UUID> participantUserIds = participantRepository.list(GameParticipantQuery.byRoom(roomId))
                .stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(GameParticipant::getId, GameParticipant::getUserId));

        Map<String, Object> boostEffects = new LinkedHashMap<>();
        List<ParticipantRoundEntry> entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId()));
        for (ParticipantRoundEntry entry : entries) {
            if (!entry.isBoostPurchased()) continue;
            List<ParticipantBarrelSelection> selections = selectionRepository.list(
                    ParticipantBarrelSelectionQuery.byEntry(entry.getId()));
            RoundScoringUtils.BoostEffect effect = RoundScoringUtils.computeBoostEffect(selections, barrelWeights);
            if (effect != null) {
                UUID participantId = entry.getParticipantId();
                UUID userId = participantUserIds.get(participantId);
                Map<String, Object> effectData = new LinkedHashMap<>();
                effectData.put("participantId", participantId.toString());
                effectData.put("userId", userId != null ? userId.toString() : null);
                effectData.put("barrelId", effect.barrelId().toString());
                effectData.put("originalWeight", effect.originalWeight());
                effectData.put("boostedWeight", effect.boostedWeight());
                boostEffects.put(participantId.toString(), effectData);
            }
        }

        Map<String, Object> weightMap = new LinkedHashMap<>();
        for (Barrel barrel : barrels) weightMap.put(barrel.getId().toString(), barrel.getWeight());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "BOOST_WINDOW_STARTED");
        payload.put("roundNumber", roundNumber);
        payload.put("barrelWeights", weightMap);
        payload.put("seedHash", roundResult.getSeedHash());
        payload.put("rawSeed", roundResult.getRawSeed());
        payload.put("boostEffects", boostEffects);
        payload.put("expiresAt", Instant.now().plusSeconds(5).toEpochMilli());
        return payload;
    }

    @Transactional
    public void markFinalistReady(UUID roomId, UUID userId) {
        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        if (room.getStatus() != GameRoomStatus.WAITING_FINALISTS_READY) {
            throw ApiException.badRequest("Room is not in finalists-ready phase");
        }

        GameParticipant participant = participantRepository.get(GameParticipantQuery.byRoomAndUser(roomId, userId));
        if (participant.getStatus() != ParticipantStatus.FINALIST) {
            throw ApiException.forbidden("Only finalists can confirm ready");
        }
        if (participant.isRound2Ready()) {
            return;
        }

        participantRepository.update(GameParticipantQuery.byId(participant.getId()), GameParticipantPatch.markRound2Ready());

        List<GameParticipant> finalists = participantRepository.list(GameParticipantQuery.byRoomAndStatus(roomId, ParticipantStatus.FINALIST));
        boolean allReady = finalists.stream().allMatch(f -> f.getId().equals(participant.getId()) || f.isRound2Ready());
        if (allReady) {
            schedulerPort.cancel(roomId, "start-round2");
            startRound(roomId, RoundConstants.ROUND_2);
        }
    }

    @Transactional
    public void startRound2AfterTimeout(UUID roomId) {
        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        if (room.getStatus() != GameRoomStatus.WAITING_FINALISTS_READY) {
            log.info("startRound2AfterTimeout: room {} is no longer in WAITING_FINALISTS_READY (status={}), skipping",
                    roomId, room.getStatus());
            return;
        }
        log.info("Finalists-ready timeout expired for room {}, starting round 2", roomId);
        startRound(roomId, RoundConstants.ROUND_2);
    }

    private void bypassRound1(UUID roomId, List<GameParticipant> participants) {
        List<String> finalistIds = new ArrayList<>();
        int autoReadyCount = 0;

        for (GameParticipant p : participants) {
            GameParticipant updated = participantRepository.update(
                    GameParticipantQuery.byId(p.getId()),
                    GameParticipantPatch.advanceToFinal());
            finalistIds.add(updated.getId().toString());
            if (updated.isBot()) {
                participantRepository.update(GameParticipantQuery.byId(updated.getId()), GameParticipantPatch.markRound2Ready());
                autoReadyCount++;
            }
        }

        notifierPort.publishGameEvent(roomId, Map.of(
                "type", "FINALISTS_ANNOUNCED",
                "finalistIds", finalistIds,
                "winCriteria", "DIRECT"
        ));

        if (autoReadyCount == participants.size()) {
            startRound(roomId, RoundConstants.ROUND_2);
        } else {
            gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(GameRoomStatus.WAITING_FINALISTS_READY));
            schedulerPort.scheduleFinalistsReadyTimeout(roomId);
        }
    }

    private void advanceToFinal(UUID roomId, List<ParticipantRoundEntry> sortedEntries, String winCriteria) {
        List<String> finalistIds = new ArrayList<>();
        List<GameParticipant> eliminated = new ArrayList<>();
        int autoReadyCount = 0;
        int finalistCount = 0;

        for (int i = 0; i < sortedEntries.size(); i++) {
            if (i < RoundConstants.FINALISTS_COUNT) {
                GameParticipant p = participantRepository.update(
                        GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()),
                        GameParticipantPatch.advanceToFinal());
                finalistIds.add(p.getId().toString());
                finalistCount++;
                if (p.isBot()) {
                    participantRepository.update(GameParticipantQuery.byId(p.getId()), GameParticipantPatch.markRound2Ready());
                    autoReadyCount++;
                }
            } else {
                GameParticipant p = participantRepository.update(
                        GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()),
                        GameParticipantPatch.eliminate());
                if (p.isRealPlayer()) {
                    eliminated.add(p);
                }
            }
        }

        notifierPort.publishGameEvent(roomId, Map.of(
                "type", "FINALISTS_ANNOUNCED",
                "finalistIds", finalistIds,
                "winCriteria", winCriteria
        ));

        if (autoReadyCount == finalistCount) {
            startRound(roomId, RoundConstants.ROUND_2);
        } else {
            gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(GameRoomStatus.WAITING_FINALISTS_READY));
            schedulerPort.scheduleFinalistsReadyTimeout(roomId);
        }

        for (GameParticipant p : eliminated) {
            gameEventPort.publishBalanceDeductReserved(p.getUserId(), p.getReservedPoints(), roomId);
        }
    }
}
