package com.vsrna.backend.application.round;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.barrel.*;
import com.vsrna.backend.domain.gameroom.*;
import com.vsrna.backend.domain.participant.*;
import com.vsrna.backend.domain.round.*;
import com.vsrna.backend.application.balance.UserBalanceService;
import com.vsrna.backend.application.prize.PrizeService;
import com.vsrna.backend.infrastructure.rng.RngResult;
import com.vsrna.backend.infrastructure.rng.RngService;
import com.vsrna.backend.infrastructure.scheduler.GameScheduler;
import com.vsrna.backend.infrastructure.websocket.GameWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final ParticipantBarrelSelectionRepository selectionRepository;
    private final UserBalanceService userBalanceService;
    private final RngService rngService;
    private final GameScheduler scheduler;
    private final GameWebSocketPublisher wsPublisher;
    @Lazy
    private final PrizeService prizeService;

    @Override
    @Transactional
    public void startRound(UUID roomId, int roundNumber) {
        GameRoomStatus newStatus = roundNumber == 1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                new GameRoomPatch(newStatus, null, null, Instant.now(), null));

        RoundResult roundResult = new RoundResult(roomId, roundNumber);
        roundResultRepository.create(roundResult);

        scheduler.scheduleRoundEnd(roomId, roundNumber, () -> resolveRound(roomId, roundNumber));

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        wsPublisher.publishRoundEvent(roomId, Map.of(
                "type", "ROUND_STARTED",
                "roundNumber", roundNumber,
                "barrelIds", barrels.stream().map(b -> b.getId().toString()).toList(),
                "timeRemaining", 30
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Barrel> getShuffledBarrels(UUID roomId, UUID userId, int roundNumber) {
        List<Barrel> barrels = new ArrayList<>(
                barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber)));
        Collections.shuffle(barrels, new Random((long) userId.hashCode() ^ roundNumber));
        return barrels;
    }

    @Override
    @Transactional
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (!config.isBoostEnabled()) {
            throw ApiException.badRequest("Boost is not enabled in this room");
        }

        GameParticipant participant = participantRepository.get(
                GameParticipantQuery.byRoomAndUser(roomId, userId));

        userBalanceService.deductPoints(userId, config.getBoostCostAmount(), roomId);

        // Увеличиваем призовой фонд на стоимость буста
        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                GameRoomPatch.prizePool(room.getPrizePoolAmount().add(config.getBoostCostAmount())));

        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        entryRepository.find(ParticipantRoundEntryQuery.byRoundResultAndParticipant(
                roundResult.getId(), participant.getId()))
                .ifPresentOrElse(
                        entry -> entryRepository.update(
                                ParticipantRoundEntryQuery.byId(entry.getId()),
                                ParticipantRoundEntryPatch.boost()),
                        () -> {
                            ParticipantRoundEntry entry = new ParticipantRoundEntry(
                                    roundResult.getId(), participant.getId());
                            entry.setBoostPurchased(true);
                            entryRepository.create(entry);
                        }
                );
    }

    @Override
    @Transactional
    public void submitSelection(UUID roomId, UUID userId, int roundNumber,
                                List<UUID> barrelIds, Instant timestamp) {
        if (barrelIds == null || barrelIds.isEmpty() || barrelIds.size() > 5) {
            throw ApiException.badRequest("Select between 1 and 5 barrels");
        }

        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedStatus = roundNumber == 1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        if (room.getStatus() != expectedStatus) {
            throw ApiException.badRequest("Round " + roundNumber + " is not in progress");
        }

        // Проверяем что все barrelIds принадлежат этой комнате и раунду
        List<Barrel> validBarrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Set<UUID> validIds = new HashSet<>();
        for (Barrel b : validBarrels) validIds.add(b.getId());
        for (UUID bid : barrelIds) {
            if (!validIds.contains(bid)) {
                throw ApiException.badRequest("Barrel " + bid + " does not belong to this round");
            }
        }

        GameParticipant participant = participantRepository.get(
                GameParticipantQuery.byRoomAndUser(roomId, userId));
        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        Optional<ParticipantRoundEntry> existing = entryRepository.find(
                ParticipantRoundEntryQuery.byRoundResultAndParticipant(
                        roundResult.getId(), participant.getId()));

        ParticipantRoundEntry entry;
        if (existing.isPresent()) {
            entry = existing.get();
            // Удаляем старые выборки и обновляем
            selectionRepository.delete(ParticipantBarrelSelectionQuery.byEntry(entry.getId()));
            entryRepository.update(
                    ParticipantRoundEntryQuery.byId(entry.getId()),
                    ParticipantRoundEntryPatch.selection(timestamp, barrelIds.size()));
        } else {
            entry = new ParticipantRoundEntry(roundResult.getId(), participant.getId());
            entry.setSelectionTimestamp(timestamp);
            entry.setSelectionCount(barrelIds.size());
            entry = entryRepository.create(entry);
        }

        List<ParticipantBarrelSelection> selections = new ArrayList<>();
        for (UUID barrelId : barrelIds) {
            selections.add(new ParticipantBarrelSelection(entry.getId(), barrelId));
        }
        selectionRepository.createAll(selections);
    }

    @Override
    @Transactional
    public void applyBoostDiscard(UUID roomId, UUID userId, int roundNumber, UUID discardedBarrelId) {
        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedBoostStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_WINDOW_1 : GameRoomStatus.BOOST_WINDOW_2;
        if (room.getStatus() != expectedBoostStatus) {
            throw ApiException.badRequest("Not in boost window for round " + roundNumber);
        }

        GameParticipant participant = participantRepository.get(
                GameParticipantQuery.byRoomAndUser(roomId, userId));
        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        ParticipantRoundEntry entry = entryRepository.get(
                ParticipantRoundEntryQuery.byRoundResultAndParticipant(
                        roundResult.getId(), participant.getId()));

        if (!entry.isBoostPurchased()) {
            throw ApiException.badRequest("Boost was not purchased for this round");
        }

        // Проверяем что discardedBarrelId входит в выборку участника
        List<ParticipantBarrelSelection> selections = selectionRepository.list(
                ParticipantBarrelSelectionQuery.byEntry(entry.getId()));
        boolean found = selections.stream().anyMatch(s -> s.getBarrelId().equals(discardedBarrelId));
        if (!found) {
            throw ApiException.badRequest("Barrel not in your selection");
        }

        entryRepository.update(
                ParticipantRoundEntryQuery.byId(entry.getId()),
                ParticipantRoundEntryPatch.discard(discardedBarrelId));
    }

    @Override
    @Transactional
    public void resolveRound(UUID roomId, int roundNumber) {
        log.info("Resolving round {} for room {}", roundNumber, roomId);

        RngResult rngResult = rngService.generate(roomId, roundNumber, 10);
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));

        // Назначаем веса бочкам
        for (int i = 0; i < barrels.size(); i++) {
            barrels.get(i).setWeight(rngResult.weights().get(i));
        }
        barrelRepository.updateAll(BarrelQuery.byRoomAndRound(roomId, roundNumber), barrels);

        // Обновляем RoundResult — СНАЧАЛА в БД, потом publish
        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                RoundResultPatch.boostWindow(rngResult.seedHash(), rngResult.seedHex()));

        // Обновляем статус комнаты
        GameRoomStatus boostStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_WINDOW_1 : GameRoomStatus.BOOST_WINDOW_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(boostStatus));

        // Публикуем веса
        Map<String, Object> weightMap = new LinkedHashMap<>();
        for (Barrel b : barrels) {
            weightMap.put(b.getId().toString(), b.getWeight());
        }
        wsPublisher.publishRoundEvent(roomId, Map.of(
                "type", "WEIGHTS_REVEALED",
                "roundNumber", roundNumber,
                "barrelWeights", weightMap,
                "seedHash", rngResult.seedHash(),
                "rawSeed", rngResult.seedHex()
        ));

        scheduler.scheduleBoostWindowEnd(roomId, roundNumber, () -> finalizeRound(roomId, roundNumber));
    }

    @Override
    @Transactional
    public void finalizeRound(UUID roomId, int roundNumber) {
        log.info("Finalizing round {} for room {}", roundNumber, roomId);

        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Map<UUID, BigDecimal> barrelWeights = new HashMap<>();
        for (Barrel b : barrels) barrelWeights.put(b.getId(), b.getWeight());

        List<ParticipantRoundEntry> entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId()));

        // Считаем totalScore для каждого участника
        for (ParticipantRoundEntry entry : entries) {
            List<ParticipantBarrelSelection> selections = selectionRepository.list(
                    ParticipantBarrelSelectionQuery.byEntry(entry.getId()));
            BigDecimal score = BigDecimal.ZERO;
            for (ParticipantBarrelSelection sel : selections) {
                if (!sel.getBarrelId().equals(entry.getDiscardedBarrelId())) {
                    BigDecimal w = barrelWeights.get(sel.getBarrelId());
                    if (w != null) score = score.add(w);
                }
            }
            entry.setTotalScore(score);
        }

        // Сортировка: totalScore DESC, selectionCount ASC, selectionTimestamp ASC
        entries.sort(Comparator
                .comparing(ParticipantRoundEntry::getTotalScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(ParticipantRoundEntry::getSelectionCount)
                .thenComparing(ParticipantRoundEntry::getSelectionTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        String winCriteria = determineWinCriteria(entries);

        for (int i = 0; i < entries.size(); i++) {
            ParticipantRoundEntry entry = entries.get(i);
            entryRepository.update(
                    ParticipantRoundEntryQuery.byId(entry.getId()),
                    ParticipantRoundEntryPatch.rank(i + 1, entry.getTotalScore()));
        }

        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                new RoundResultPatch(RoundResultStatus.COMPLETED, null, null, Instant.now()));

        wsPublisher.publishRoundEvent(roomId, Map.of(
                "type", "ROUND_COMPLETED",
                "roundNumber", roundNumber,
                "winnerId", entries.isEmpty() ? "" : entries.get(0).getParticipantId().toString(),
                "winCriteria", winCriteria
        ));

        if (roundNumber == 1) {
            advanceToFinal(roomId, entries, winCriteria);
        } else {
            prizeService.distributePrize(roomId);
        }
    }

    private void advanceToFinal(UUID roomId, List<ParticipantRoundEntry> sortedEntries, String winCriteria) {
        List<String> finalistIds = new ArrayList<>();
        for (int i = 0; i < sortedEntries.size(); i++) {
            GameParticipantPatch patch = i < 2
                    ? GameParticipantPatch.advanceToFinal()
                    : GameParticipantPatch.eliminate();
            participantRepository.update(
                    GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()), patch);

            if (i < 2) {
                finalistIds.add(sortedEntries.get(i).getParticipantId().toString());
            } else {
                // Списываем резерв у выбывших реальных игроков
                GameParticipant p = participantRepository.get(
                        GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()));
                if (!p.isBot() && p.getUserId() != null) {
                    userBalanceService.deductReserved(p.getUserId(), p.getReservedPoints(), roomId);
                }
            }
        }

        wsPublisher.publishGameEvent(roomId, Map.of(
                "type", "FINALISTS_ANNOUNCED",
                "finalistIds", finalistIds,
                "winCriteria", winCriteria
        ));

        startRound(roomId, 2);
    }

    private String determineWinCriteria(List<ParticipantRoundEntry> sorted) {
        if (sorted.size() < 2) return "SCORE";
        ParticipantRoundEntry first = sorted.get(0);
        ParticipantRoundEntry second = sorted.get(1);
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
