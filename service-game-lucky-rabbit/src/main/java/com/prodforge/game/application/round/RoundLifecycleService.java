package com.prodforge.game.application.round;

import com.prodforge.game.application.bot.BotService;
import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.metrics.GameMetrics;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.application.port.GameEventTypes;
import com.prodforge.game.application.port.GameNotifierPort;
import com.prodforge.game.application.port.GamePhase;
import com.prodforge.game.application.port.GameSchedulerPort;
import com.prodforge.game.application.prize.PrizeService;
import com.prodforge.game.application.round.scoring.RoundConstants;
import com.prodforge.game.application.round.scoring.RoundScoringUtils;
import com.prodforge.game.domain.barrel.Barrel;
import com.prodforge.game.domain.barrel.BarrelQuery;
import com.prodforge.game.domain.barrel.BarrelRepository;
import com.prodforge.game.domain.gameroom.GameRoomPatch;
import com.prodforge.game.domain.gameroom.GameRoomQuery;
import com.prodforge.game.domain.gameroom.GameRoomRepository;
import com.prodforge.game.domain.gameroom.GameRoomStatus;
import com.prodforge.game.domain.history.GameHistoryAnalyticsRepository;
import com.prodforge.game.domain.participant.GameParticipant;
import com.prodforge.game.domain.participant.GameParticipantPatch;
import com.prodforge.game.domain.participant.GameParticipantQuery;
import com.prodforge.game.domain.participant.GameParticipantRepository;
import com.prodforge.game.domain.participant.ParticipantStatus;
import com.prodforge.game.domain.rng.RngCommitment;
import com.prodforge.game.domain.rng.RngPort;
import com.prodforge.game.domain.round.ParticipantBarrelSelection;
import com.prodforge.game.domain.round.ParticipantBarrelSelectionQuery;
import com.prodforge.game.domain.round.ParticipantBarrelSelectionRepository;
import com.prodforge.game.domain.round.ParticipantRoundEntry;
import com.prodforge.game.domain.round.ParticipantRoundEntryPatch;
import com.prodforge.game.domain.round.ParticipantRoundEntryQuery;
import com.prodforge.game.domain.round.ParticipantRoundEntryRepository;
import com.prodforge.game.domain.round.RoundResult;
import com.prodforge.game.domain.round.RoundResultPatch;
import com.prodforge.game.domain.round.RoundResultQuery;
import com.prodforge.game.domain.round.RoundResultRepository;
import com.prodforge.game.domain.round.RoundResultStatus;
import com.prodforge.game.domain.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final GameHistoryAnalyticsRepository gameHistoryAnalyticsRepository;
    private final BotService botService;
    private final GameEventPort gameEventPort;
    private final GameEventLogService gameEventLogService;
    private final GameMetrics gameMetrics;

    /**
     * Запускает раунд (1 или 2).
     *
     * Поток:
     *   1. Если раунд 1 и активных игроков ≤ FINALISTS_COUNT → пропускаем раунд 1,
     *      все сразу становятся финалистами (bypassRound1).
     *   2. Меняем статус комнаты на ROUND_1 / ROUND_2.
     *   3. Генерируем RNG commitment: seed скрыт в БД, клиентам публикуется только его хэш.
     *   4. Планируем таймер окончания раунда (Quartz job).
     *   5. Публикуем WebSocket-событие с ID бочек и seedHash — игроки начинают выбор.
     */
    @Transactional
    public void startRound(UUID roomId, int roundNumber) {
        log.info("Starting round {} for room {}", roundNumber, roomId);

        if (roundNumber == RoundConstants.ROUND_1) {
            List<GameParticipant> active = participantRepository.list(
                    GameParticipantQuery.byRoomAndStatus(roomId, ParticipantStatus.ACTIVE));
            if (active.size() <= RoundConstants.FINALISTS_COUNT) {
                log.info("Room {} has only {} active participants, bypassing round 1", roomId, active.size());
                bypassRound1(roomId, active);
                return;
            }
        }

        GameRoomStatus newStatus = roundNumber == RoundConstants.ROUND_1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                new GameRoomPatch(newStatus, null, null, Instant.now(), null, null));

        RngCommitment commitment = rngPort.commit(roomId, roundNumber);
        RoundResult roundResult = roundResultRepository.create(new RoundResult(roomId, roundNumber));
        // Сохраняем и хэш (публичный), и rawSeed (приватный до reveal)
        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                RoundResultPatch.commit(commitment.seedHash(), commitment.rawSeed()));

        Instant roundExpiresAt = schedulerPort.scheduleRoundEnd(roomId, roundNumber);

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        notifierPort.publishRoundEvent(roomId, Map.of(
                GameEventTypes.FIELD_TYPE, GameEventTypes.ROUND_STARTED,
                "roundNumber", roundNumber,
                "barrelIds", barrels.stream().map(barrel -> barrel.getId().toString()).toList(),
                "seedHash", commitment.seedHash(),
                "expiresAt", roundExpiresAt.toEpochMilli()
        ));
        log.info("Round {} started for room {}, {} barrels, seedHash={}", roundNumber, roomId, barrels.size(), commitment.seedHash());
        gameEventLogService.log(roomId, GameEventTypes.ROUND_STARTED, Map.of("round", roundNumber));
        (roundNumber == RoundConstants.ROUND_1 ? gameMetrics.round1Started : gameMetrics.round2Started).increment();
    }

    /**
     * Раскрывает веса бочек по истечении времени выбора.
     *
     * Поток:
     *   1. Достаём rawSeed из БД и передаём в RNG — получаем 12 весов [-10, +10].
     *   2. Присваиваем веса бочкам в порядке их создания (индекс = порядковый номер).
     *   3. Меняем статус комнаты на BOOST_DECISION — открывается окно для покупки буста.
     *   4. Планируем таймер окончания boost decision.
     *   5. Публикуем событие BOOST_DECISION_STARTED (без весов — клиент ещё не видит результаты).
     */
    @Transactional
    public void resolveRound(UUID roomId, int roundNumber) {
        log.info("Resolving round {} for room {}", roundNumber, roomId);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<BigDecimal> weights = rngPort.reveal(roundResult.getRawSeed(), RoundConstants.BARRELS_PER_ROUND);

        // Веса назначаются по порядку: бочка[0] → weight[0], бочка[1] → weight[1], ...
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        for (int barrelIndex = 0; barrelIndex < barrels.size(); barrelIndex++) {
            barrels.get(barrelIndex).setWeight(weights.get(barrelIndex));
        }
        barrelRepository.updateAll(BarrelQuery.byRoomAndRound(roomId, roundNumber), barrels);

        Map<String, Object> weightsMap = new LinkedHashMap<>();
        for (Barrel barrel : barrels) weightsMap.put(barrel.getId().toString(), barrel.getWeight());
        gameEventLogService.log(roomId, GameEventTypes.WEIGHTS_REVEALED, Map.of("round", roundNumber, "weights", weightsMap));

        GameRoomStatus decisionStatus = roundNumber == RoundConstants.ROUND_1
                ? GameRoomStatus.BOOST_DECISION_1 : GameRoomStatus.BOOST_DECISION_2;
        gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(decisionStatus));

        Instant boostDecisionExpiresAt = schedulerPort.scheduleBoostDecisionEnd(roomId, roundNumber);

        notifierPort.publishRoundEvent(roomId, Map.of(
                GameEventTypes.FIELD_TYPE, GameEventTypes.BOOST_DECISION_STARTED,
                "roundNumber", roundNumber,
                "expiresAt", boostDecisionExpiresAt.toEpochMilli()
        ));
    }

    /**
     * Открывает окно буста: публикует веса бочек и rawSeed клиентам.
     *
     * После этого события:
     *   - Игроки видят веса своих бочек и могут проверить seed.
     *   - Игроки могут купить буст (BoostService.purchaseBoost).
     *   - В payload включены уже рассчитанные эффекты буста для тех, кто успел купить раньше.
     */
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

        Instant boostWindowExpiresAt = schedulerPort.scheduleBoostWindowEnd(roomId, roundNumber);

        Map<String, Object> payload = buildBoostWindowPayload(roomId, roundNumber, roundResult, barrels, barrelWeights, boostWindowExpiresAt);
        notifierPort.publishRoundEvent(roomId, payload);
    }

    /**
     * Финализирует раунд: подсчитывает очки, ранжирует участников, определяет победителей/выбывших.
     *
     * Поток:
     *   1. Боты делают выбор бочек (с учётом protectionMode).
     *   2. Добавляем нулевые записи для игроков, не сделавших выбор.
     *   3. Выбываем игроков без выбора (статус ELIMINATED, списываем зарезервированные баллы).
     *   4. Считаем очки каждому: сумма весов выбранных бочек + эффект буста.
     *   5. Сортируем по убыванию очков; тай-брейк: меньше бочек → лучше; при равенстве → раньше выбрал.
     *   6. Сохраняем ранги в БД.
     *   7. Публикуем ROUND_COMPLETED с победителем и критерием победы.
     *   8. Если раунд 1 → переводим топ-2 в финал (advanceToFinal).
     *      Если раунд 2 → распределяем призы (prizeService.distributePrize).
     */
    @Transactional
    public void finalizeRound(UUID roomId, int roundNumber) {
        log.info("Finalizing round {} for room {}: scoring entries and determining winner", roundNumber, roomId);

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Map<UUID, BigDecimal> barrelWeights = buildBarrelWeightMap(barrels);

        // protectionMode = true если система в убытке: боты будут выбирать лучшие бочки
        boolean protectionMode = gameHistoryAnalyticsRepository.getCumulativeSystemBalance()
                .compareTo(BigDecimal.ZERO) < 0;
        botService.submitBotSelections(roomId, roundNumber, protectionMode, barrelWeights);

        List<ParticipantRoundEntry> entries = new ArrayList<>(entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId())));

        List<UUID> entryIds = entries.stream().map(ParticipantRoundEntry::getId).toList();
        Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry = selectionRepository
                .listByEntries(entryIds).stream()
                .collect(Collectors.groupingBy(ParticipantBarrelSelection::getEntryId));

        // Участники без записи (не открывали раунд) получают пустую запись с нулевым счётом
        entries = addDefaultEntriesForAbsentParticipants(entries, roundResult.getId(), roomId, roundNumber);

        List<String> disqualifiedIds = new ArrayList<>();
        // Участники без выбора дисквалифицируются и не участвуют в ранжировании
        entries = eliminateNoSelectionParticipants(entries, selectionsByEntry, roomId, disqualifiedIds);

        scoreAndRankEntries(entries, selectionsByEntry, barrelWeights);
        logParticipantScores(roomId, roundNumber, entries, selectionsByEntry, barrelWeights);
        String winCriteria = RoundScoringUtils.determineWinCriteria(entries);

        for (int rank = 0; rank < entries.size(); rank++) {
            ParticipantRoundEntry entry = entries.get(rank);
            entryRepository.update(
                    ParticipantRoundEntryQuery.byId(entry.getId()),
                    ParticipantRoundEntryPatch.rank(rank + 1, entry.getTotalScore()));
        }

        roundResultRepository.update(
                RoundResultQuery.byId(roundResult.getId()),
                new RoundResultPatch(RoundResultStatus.COMPLETED, null, null, Instant.now()));

        String winnerIdStr = entries.isEmpty() ? "" : entries.get(0).getParticipantId().toString();

        Map<String, Object> roundCompletedPayload = new LinkedHashMap<>();
        roundCompletedPayload.put(GameEventTypes.FIELD_TYPE, GameEventTypes.ROUND_COMPLETED);
        roundCompletedPayload.put("roundNumber", roundNumber);
        roundCompletedPayload.put("winnerId", winnerIdStr);
        roundCompletedPayload.put("winCriteria", winCriteria);
        roundCompletedPayload.put("disqualifiedIds", disqualifiedIds);
        notifierPort.publishRoundEvent(roomId, roundCompletedPayload);

        log.info("Round {} completed for room {}: winner={}, criteria={}, participants={}",
                roundNumber, roomId, winnerIdStr.isEmpty() ? "none" : winnerIdStr, winCriteria, entries.size());
        gameEventLogService.log(roomId, GameEventTypes.ROUND_COMPLETED,
                Map.of("round", roundNumber, "winCriteria", winCriteria));
        gameMetrics.recordRoundCompleted(roundNumber, winCriteria);

        if (roundNumber == RoundConstants.ROUND_1) {
            advanceToFinal(roomId, entries, winCriteria);
        } else {
            prizeService.distributePrize(roomId);
        }
    }

    /**
     * Создаёт пустые записи участника для тех, кто не сделал ни одного выбора.
     * Такие участники будут дисквалифицированы в eliminateNoSelectionParticipants.
     */
    private List<ParticipantRoundEntry> addDefaultEntriesForAbsentParticipants(
            List<ParticipantRoundEntry> entries, UUID roundResultId, UUID roomId, int roundNumber) {
        ParticipantStatus statusForRound = roundNumber == RoundConstants.ROUND_1
                ? ParticipantStatus.ACTIVE : ParticipantStatus.FINALIST;
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

    /**
     * Выбывает участников, не выбравших ни одной бочки.
     * Реальным игрокам списываются зарезервированные баллы через BalancePort.
     * Возвращает только участников с выбором.
     */
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

        if (!disqualifiedRealPlayers.isEmpty()) {
            gameMetrics.playersDisqualified.increment(disqualifiedRealPlayers.size());
        }
        for (GameParticipant player : disqualifiedRealPlayers) {
            gameEventPort.publishBalanceDeductReserved(player.getUserId(), player.getReservedPoints(), roomId);
        }
        return remaining;
    }

    /**
     * Считает очки и сортирует записи участников.
     *
     * Сортировка (порядок приоритетов):
     *   1. totalScore DESC  — больше очков → выше место.
     *   2. selectionTimestamp ASC — при равных очках: кто раньше выбрал → выше место.
     *      (selectionCount учитывается в determineWinCriteria, но НЕ в сортировке здесь)
     */
    private void scoreAndRankEntries(List<ParticipantRoundEntry> entries,
                                     Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry,
                                     Map<UUID, BigDecimal> barrelWeights) {
        for (ParticipantRoundEntry entry : entries) {
            List<ParticipantBarrelSelection> selections = selectionsByEntry.getOrDefault(entry.getId(), List.of());
            entry.setTotalScore(RoundScoringUtils.calculateScore(entry, selections, barrelWeights));
        }

        entries.sort(Comparator
                .comparing(ParticipantRoundEntry::getTotalScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ParticipantRoundEntry::getSelectionTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private void logParticipantScores(UUID roomId, int roundNumber,
                                       List<ParticipantRoundEntry> entries,
                                       Map<UUID, List<ParticipantBarrelSelection>> selectionsByEntry,
                                       Map<UUID, BigDecimal> barrelWeights) {
        for (ParticipantRoundEntry entry : entries) {
            List<ParticipantBarrelSelection> selections = selectionsByEntry.getOrDefault(entry.getId(), List.of());

            List<String> barrelIds = selections.stream()
                    .map(selection -> selection.getBarrelId().toString())
                    .toList();
            BigDecimal totalRaw = selections.stream()
                    .map(selection -> barrelWeights.getOrDefault(selection.getBarrelId(), BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("participantId", entry.getParticipantId().toString());
            detail.put("round", roundNumber);
            detail.put("score", entry.getTotalScore());
            detail.put("barrels", barrelIds);
            detail.put("totalWeight", totalRaw);

            if (entry.isBoostPurchased()) {
                RoundScoringUtils.BoostEffect effect = RoundScoringUtils.computeBoostEffect(selections, barrelWeights);
                if (effect != null) {
                    detail.put("boost", Map.of(
                            "barrelId", effect.barrelId().toString(),
                            "from", effect.originalWeight(),
                            "to", effect.boostedWeight()
                    ));
                } else {
                    detail.put("boost", "purchased,no-effect");
                }
            }

            gameEventLogService.log(roomId, GameEventTypes.PARTICIPANT_SCORED, detail);
        }
    }

    private Map<UUID, BigDecimal> buildBarrelWeightMap(List<Barrel> barrels) {
        Map<UUID, BigDecimal> weights = new HashMap<>(barrels.size() * 2);
        for (Barrel barrel : barrels) weights.put(barrel.getId(), barrel.getWeight());
        return weights;
    }

    private Map<String, Object> buildBoostWindowPayload(UUID roomId, int roundNumber,
                                                         RoundResult roundResult,
                                                         List<Barrel> barrels,
                                                         Map<UUID, BigDecimal> barrelWeights,
                                                         Instant expiresAt) {
        Map<UUID, UUID> participantUserIds = participantRepository.list(GameParticipantQuery.byRoom(roomId))
                .stream()
                .filter(participant -> participant.getUserId() != null)
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
        payload.put(GameEventTypes.FIELD_TYPE, GameEventTypes.BOOST_WINDOW_STARTED);
        payload.put("roundNumber", roundNumber);
        payload.put("barrelWeights", weightMap);
        payload.put("seedHash", roundResult.getSeedHash());
        // rawSeed раскрывается здесь — игроки могут проверить SHA-256(rawSeed) == seedHash
        payload.put("rawSeed", roundResult.getRawSeed());
        payload.put("boostEffects", boostEffects);
        payload.put("expiresAt", expiresAt.toEpochMilli());
        return payload;
    }

    /**
     * Обрабатывает подтверждение готовности финалиста к раунду 2.
     * Как только оба финалиста подтвердили готовность — раунд 2 стартует немедленно,
     * не дожидаясь таймаута.
     */
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
        boolean allReady = finalists.stream().allMatch(finalist ->
                finalist.getId().equals(participant.getId()) || finalist.isRound2Ready());
        if (allReady) {
            schedulerPort.cancel(roomId, GamePhase.START_ROUND2);
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

    /**
     * Пропускает раунд 1, если игроков не больше, чем мест в финале.
     * Все активные участники сразу становятся финалистами.
     * Боты автоматически помечаются как готовые к раунду 2.
     */
    private void bypassRound1(UUID roomId, List<GameParticipant> participants) {
        RoundResult skippedRound = roundResultRepository.create(new RoundResult(roomId, RoundConstants.ROUND_1));
        roundResultRepository.update(
                RoundResultQuery.byId(skippedRound.getId()),
                RoundResultPatch.completed(Instant.now()));

        List<String> finalistIds = new ArrayList<>();
        int autoReadyCount = 0;

        for (GameParticipant participant : participants) {
            GameParticipant updated = participantRepository.update(
                    GameParticipantQuery.byId(participant.getId()),
                    GameParticipantPatch.advanceToFinal());
            finalistIds.add(updated.getId().toString());
            if (updated.isBot()) {
                participantRepository.update(GameParticipantQuery.byId(updated.getId()), GameParticipantPatch.markRound2Ready());
                autoReadyCount++;
            }
        }

        notifierPort.publishGameEvent(roomId, Map.of(
                GameEventTypes.FIELD_TYPE, GameEventTypes.FINALISTS_ANNOUNCED,
                "finalistIds", finalistIds,
                "winCriteria", RoundScoringUtils.WIN_CRITERIA_DIRECT
        ));

        if (autoReadyCount == participants.size()) {
            startRound(roomId, RoundConstants.ROUND_2);
        } else {
            gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(GameRoomStatus.WAITING_FINALISTS_READY));
            schedulerPort.scheduleFinalistsReadyTimeout(roomId);
        }
    }

    /**
     * Переводит топ-FINALISTS_COUNT участников в финал, остальных выбывает.
     * Реальным проигравшим списываются зарезервированные баллы.
     * Если все финалисты — боты, раунд 2 стартует немедленно.
     */
    private void advanceToFinal(UUID roomId, List<ParticipantRoundEntry> sortedEntries, String winCriteria) {
        List<String> finalistIds = new ArrayList<>();
        List<GameParticipant> eliminated = new ArrayList<>();
        int autoReadyCount = 0;
        int finalistCount = 0;

        for (int entryIndex = 0; entryIndex < sortedEntries.size(); entryIndex++) {
            if (entryIndex < RoundConstants.FINALISTS_COUNT) {
                GameParticipant finalist = participantRepository.update(
                        GameParticipantQuery.byId(sortedEntries.get(entryIndex).getParticipantId()),
                        GameParticipantPatch.advanceToFinal());
                finalistIds.add(finalist.getId().toString());
                finalistCount++;
                if (finalist.isBot()) {
                    participantRepository.update(GameParticipantQuery.byId(finalist.getId()), GameParticipantPatch.markRound2Ready());
                    autoReadyCount++;
                }
            } else {
                GameParticipant loser = participantRepository.update(
                        GameParticipantQuery.byId(sortedEntries.get(entryIndex).getParticipantId()),
                        GameParticipantPatch.eliminate());
                if (loser.isRealPlayer()) {
                    eliminated.add(loser);
                }
            }
        }

        notifierPort.publishGameEvent(roomId, Map.of(
                GameEventTypes.FIELD_TYPE, GameEventTypes.FINALISTS_ANNOUNCED,
                "finalistIds", finalistIds,
                "winCriteria", winCriteria
        ));
        gameEventLogService.log(roomId, GameEventTypes.FINALISTS_ANNOUNCED,
                Map.of("finalists", finalistIds, "criteria", winCriteria));

        if (autoReadyCount == finalistCount) {
            startRound(roomId, RoundConstants.ROUND_2);
        } else {
            gameRoomRepository.update(GameRoomQuery.byId(roomId), GameRoomPatch.status(GameRoomStatus.WAITING_FINALISTS_READY));
            schedulerPort.scheduleFinalistsReadyTimeout(roomId);
        }

        for (GameParticipant loser : eliminated) {
            gameEventPort.publishBalanceDeductReserved(loser.getUserId(), loser.getReservedPoints(), roomId);
        }
    }
}
