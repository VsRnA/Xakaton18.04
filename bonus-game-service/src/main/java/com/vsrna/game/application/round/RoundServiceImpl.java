package com.vsrna.game.application.round;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.port.GameSchedulerPort;
import com.vsrna.game.application.prize.PrizeService;
import com.vsrna.game.domain.barrel.*;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.*;
import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.domain.history.GameHistoryQuery;
import com.vsrna.game.domain.history.GameHistoryRepository;
import com.vsrna.game.domain.participant.*;
import com.vsrna.game.domain.rng.RngCommitment;
import com.vsrna.game.domain.rng.RngPort;
import com.vsrna.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
    private final BalancePort balancePort;
    private final RngPort rngPort;
    private final GameSchedulerPort schedulerPort;
    private final GameNotifierPort notifierPort;
    private final PrizeService prizeService;
    private final GameHistoryRepository gameHistoryRepository;
    private final BotService botService;

    @Override
    @Transactional
    public void startRound(UUID roomId, int roundNumber) {
        GameRoomStatus newStatus = roundNumber == 1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                new GameRoomPatch(newStatus, null, null, Instant.now(), null, null));

        // Фаза 1 commit: генерируем seed ДО выборов игроков.
        // seedHash публикуется игрокам — это доказательство честности.
        // rawSeed хранится в БД; раскрывается в resolveRound после выборов.
        RngCommitment commitment = rngPort.commit(roomId, roundNumber);
        RoundResult roundResult = new RoundResult(roomId, roundNumber);
        roundResult = roundResultRepository.create(roundResult);
        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                RoundResultPatch.commit(commitment.seedHash(), commitment.rawSeed()));

        schedulerPort.scheduleRoundEnd(roomId, roundNumber);

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        notifierPort.publishRoundEvent(roomId, Map.of(
                "type", "ROUND_STARTED",
                "roundNumber", roundNumber,
                "barrelIds", barrels.stream().map(b -> b.getId().toString()).toList(),
                "seedHash", commitment.seedHash(),   // ← commitment: игрок видит hash до выборов
                "expiresAt", Instant.now().plusSeconds(30).toEpochMilli()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Barrel> getShuffledBarrels(UUID roomId, UUID userId, int roundNumber) {
        List<Barrel> barrels = new ArrayList<>(
                barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber)));
        long seed = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
        seed ^= ((long) roundNumber << 32);
        Collections.shuffle(barrels, new Random(seed));
        return barrels;
    }

    @Override
    @Transactional
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (!config.isBoostEnabled()) {
            throw ApiException.badRequest("Boost is not enabled in this room");
        }

        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedDecisionStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_DECISION_1 : GameRoomStatus.BOOST_DECISION_2;
        if (room.getStatus() != expectedDecisionStatus) {
            throw ApiException.badRequest("Boost can only be purchased during the boost decision window");
        }

        GameParticipant participant = participantRepository.get(
                GameParticipantQuery.byRoomAndUser(roomId, userId));

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

        // HTTP-вызов после коммита: если транзакция откатится — деньги не спишутся.
        final UUID finalUserId = userId;
        final BigDecimal finalBoostCost = config.getBoostCostAmount();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    balancePort.deduct(finalUserId, finalBoostCost, roomId);
                } catch (Exception e) {
                    log.error("COMPENSATION NEEDED: failed to deduct boost balance after commit " +
                              "userId={}, roomId={}: {}", finalUserId, roomId, e.getMessage());
                }
            }
        });
    }

    @Override
    @Transactional
    public void submitSelection(UUID roomId, UUID userId, int roundNumber,
                                List<UUID> barrelIds, Instant timestamp) {
        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (barrelIds == null || barrelIds.isEmpty() || barrelIds.size() > config.getMaxBarrelSelection()) {
            throw ApiException.badRequest("Select between 1 and " + config.getMaxBarrelSelection() + " barrels");
        }

        GameRoom room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedStatus = roundNumber == 1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        if (room.getStatus() != expectedStatus) {
            throw ApiException.badRequest("Round " + roundNumber + " is not in progress");
        }

        List<Barrel> validBarrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Set<UUID> validIds = validBarrels.stream().map(Barrel::getId).collect(Collectors.toSet());
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

        UUID entryId;
        if (existing.isPresent()) {
            ParticipantRoundEntry entry = existing.get();
            entryId = entry.getId();
            selectionRepository.delete(ParticipantBarrelSelectionQuery.byEntry(entryId));
            entryRepository.update(
                    ParticipantRoundEntryQuery.byId(entryId),
                    ParticipantRoundEntryPatch.selection(timestamp, barrelIds.size()));
        } else {
            ParticipantRoundEntry newEntry = new ParticipantRoundEntry(roundResult.getId(), participant.getId());
            newEntry.setSelectionTimestamp(timestamp);
            newEntry.setSelectionCount(barrelIds.size());
            entryId = entryRepository.create(newEntry).getId();
        }

        List<ParticipantBarrelSelection> selections = barrelIds.stream()
                .map(barrelId -> new ParticipantBarrelSelection(entryId, barrelId))
                .toList();
        selectionRepository.createAll(selections);

        int selectedCount = entryRepository.countByRoundResult(roundResult.getId());
        int totalPlayers = participantRepository.count(GameParticipantQuery.byRoom(roomId));
        notifierPort.publishRoundEvent(roomId, Map.of(
                "type", "PLAYER_SELECTED",
                "roundNumber", roundNumber,
                "selectedCount", selectedCount,
                "totalPlayers", totalPlayers
        ));
    }

    @Override
    @Transactional
    public void applyBoost(UUID roomId, UUID userId, int roundNumber, UUID boostedBarrelId) {
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

        List<ParticipantBarrelSelection> selections = selectionRepository.list(
                ParticipantBarrelSelectionQuery.byEntry(entry.getId()));
        boolean found = selections.stream().anyMatch(s -> s.getBarrelId().equals(boostedBarrelId));
        if (!found) {
            throw ApiException.badRequest("Barrel not in your selection");
        }

        entryRepository.update(
                ParticipantRoundEntryQuery.byId(entry.getId()),
                ParticipantRoundEntryPatch.applyBoost(boostedBarrelId));
    }

    @Override
    @Transactional
    public void resolveRound(UUID roomId, int roundNumber) {
        log.info("Resolving round {} for room {}", roundNumber, roomId);

        // Фаза 2 reveal: используем rawSeed, сохранённый в startRound (фаза commit).
        // Детерминированно восстанавливаем веса — результат тот же, что был бы при генерации.
        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<BigDecimal> weights = rngPort.reveal(roundResult.getRawSeed(), 10);

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        for (int i = 0; i < barrels.size(); i++) {
            barrels.get(i).setWeight(weights.get(i));
        }
        barrelRepository.updateAll(BarrelQuery.byRoomAndRound(roomId, roundNumber), barrels);

        GameRoomStatus decisionStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_DECISION_1 : GameRoomStatus.BOOST_DECISION_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(decisionStatus));

        // Публикуем rawSeed — это reveal: игрок может проверить SHA256(rawSeed) == seedHash из ROUND_STARTED.
        // Затем у игрока есть boost-decision-seconds секунд, чтобы решить, покупать ли буст.
        Map<String, Object> weightsPayload = new LinkedHashMap<>();
        weightsPayload.put("type", "WEIGHTS_REVEALED");
        weightsPayload.put("roundNumber", roundNumber);
        Map<String, Object> weightMap = new LinkedHashMap<>();
        for (Barrel b : barrels) weightMap.put(b.getId().toString(), b.getWeight());
        weightsPayload.put("barrelWeights", weightMap);
        weightsPayload.put("seedHash", roundResult.getSeedHash());
        weightsPayload.put("rawSeed", roundResult.getRawSeed());   // ← reveal
        weightsPayload.put("phase", "BOOST_DECISION");
        weightsPayload.put("expiresAt", Instant.now().plusSeconds(5).toEpochMilli());
        notifierPort.publishRoundEvent(roomId, weightsPayload);

        schedulerPort.scheduleBoostDecisionEnd(roomId, roundNumber);
    }

    @Override
    @Transactional
    public void startBoostWindow(UUID roomId, int roundNumber) {
        log.info("Starting boost window for room {} round {}", roomId, roundNumber);

        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                RoundResultPatch.boostWindow());

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

    @Override
    @Transactional
    public void finalizeRound(UUID roomId, int roundNumber) {
        log.info("Finalizing round {} for room {}", roundNumber, roomId);

        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Map<UUID, BigDecimal> barrelWeights = new HashMap<>();
        for (Barrel b : barrels) barrelWeights.put(b.getId(), b.getWeight());

        // Боты выбирают бочки после раскрытия весов.
        // В режиме защиты маржи (кумулятивный баланс системы < 0) — выбирают оптимально.
        boolean protectionMode = gameHistoryRepository.getCumulativeSystemBalance()
                .compareTo(java.math.BigDecimal.ZERO) < 0;
        botService.submitBotSelections(roomId, roundNumber, protectionMode, barrelWeights);

        List<ParticipantRoundEntry> entries = new ArrayList<>(entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId())));

        // Загружаем все selections одним запросом (fix N+1)
        List<UUID> entryIds = entries.stream().map(ParticipantRoundEntry::getId).toList();
        Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry = selectionRepository
                .listByEntries(entryIds).stream()
                .collect(Collectors.groupingBy(ParticipantBarrelSelection::getEntryId));

        for (ParticipantRoundEntry entry : entries) {
            List<ParticipantBarrelSelection> sels = selectionsByEntry.getOrDefault(entry.getId(), List.of());
            BigDecimal score = BigDecimal.ZERO;
            for (ParticipantBarrelSelection sel : sels) {
                BigDecimal w = barrelWeights.get(sel.getBarrelId());
                if (w == null) continue;
                if (sel.getBarrelId().equals(entry.getBoostedBarrelId())) {
                    // Буст: отрицательный вес → меняем знак (добавляем |w|),
                    //        положительный вес → умножаем на 1.5,
                    //        нулевой вес → ничего не меняется.
                    if (w.signum() < 0) {
                        score = score.add(w.negate());
                    } else if (w.signum() > 0) {
                        score = score.add(w.multiply(new BigDecimal("1.5")));
                    }
                } else {
                    score = score.add(w);
                }
            }
            entry.setTotalScore(score);
        }

        entries.sort(Comparator
                .comparing(ParticipantRoundEntry::getTotalScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(ParticipantRoundEntry::getSelectionCount)
                .thenComparing(ParticipantRoundEntry::getSelectionTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())));

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

        notifierPort.publishRoundEvent(roomId, Map.of(
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
        List<GameParticipant> eliminated = new ArrayList<>();

        for (int i = 0; i < sortedEntries.size(); i++) {
            GameParticipantPatch patch = i < 2
                    ? GameParticipantPatch.advanceToFinal()
                    : GameParticipantPatch.eliminate();
            participantRepository.update(
                    GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()), patch);

            if (i < 2) {
                finalistIds.add(sortedEntries.get(i).getParticipantId().toString());
            } else {
                GameParticipant p = participantRepository.get(
                        GameParticipantQuery.byId(sortedEntries.get(i).getParticipantId()));
                if (!p.isBot() && p.getUserId() != null) {
                    eliminated.add(p);
                }
            }
        }

        notifierPort.publishGameEvent(roomId, Map.of(
                "type", "FINALISTS_ANNOUNCED",
                "finalistIds", finalistIds,
                "winCriteria", winCriteria
        ));

        startRound(roomId, 2);

        // HTTP-вызовы для возврата средств выбывшим — после коммита всех DB-операций раунда.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (GameParticipant p : eliminated) {
                    try {
                        balancePort.release(p.getUserId(), p.getReservedPoints(), roomId);
                    } catch (Exception e) {
                        log.error("COMPENSATION NEEDED: failed to release balance for eliminated " +
                                  "userId={}, roomId={}: {}", p.getUserId(), roomId, e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public RoundResultDetails getRoundResult(UUID roomId, int roundNumber) {
        RoundResult roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<ParticipantRoundEntry> entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId()));

        // Bulk-загрузка: один запрос вместо N (был N+1 в контроллере)
        Map<UUID, GameParticipant> participantMap = participantRepository
                .list(GameParticipantQuery.byRoom(roomId)).stream()
                .collect(Collectors.toMap(GameParticipant::getId, p -> p));

        List<RoundResultDetails.ParticipantScore> scores = entries.stream()
                .map(e -> {
                    GameParticipant p = participantMap.get(e.getParticipantId());
                    return new RoundResultDetails.ParticipantScore(
                            e.getParticipantId(),
                            p != null && p.isBot(),
                            e.getTotalScore(),
                            e.getSelectionCount(),
                            e.getRankInRound()
                    );
                })
                .toList();

        UUID winnerId = entries.stream()
                .filter(e -> e.getRankInRound() != null && e.getRankInRound() == 1)
                .map(ParticipantRoundEntry::getParticipantId)
                .findFirst().orElse(null);

        return new RoundResultDetails(roundResult, scores, winnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public GameHistory getGameHistory(UUID roomId) {
        return gameHistoryRepository.get(GameHistoryQuery.byRoom(roomId));
    }
}
