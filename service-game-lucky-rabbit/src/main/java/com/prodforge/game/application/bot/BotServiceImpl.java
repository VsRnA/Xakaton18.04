package com.prodforge.game.application.bot;

import com.prodforge.game.application.round.scoring.RoundConstants;
import com.prodforge.game.domain.barrel.*;
import com.prodforge.game.domain.gameroom.GameRoomConfig;
import com.prodforge.game.domain.gameroom.GameRoomConfigQuery;
import com.prodforge.game.domain.gameroom.GameRoomConfigRepository;
import com.prodforge.game.domain.participant.*;
import com.prodforge.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final ParticipantBarrelSelectionRepository selectionRepository;

    private static final String BOT_NAME_FORMAT = "Бот %d";

    private final SecureRandom rng = new SecureRandom();

    @Override
    @Transactional
    public List<GameParticipant> createBotsForRoom(UUID roomId, int count, BigDecimal entryFeeAmount) {
        List<GameParticipant> bots = new ArrayList<>();
        for (int botNumber = 1; botNumber <= count; botNumber++) {
            // userId = null — отличительный признак бота (нет реального пользователя)
            GameParticipant bot = new GameParticipant(roomId, null, true,
                    String.format(BOT_NAME_FORMAT, botNumber), entryFeeAmount);
            bots.add(participantRepository.create(bot));
        }
        return bots;
    }

    /**
     * Боты делают выбор бочек в момент финализации раунда, после того как веса уже известны.
     * Выбор записывается в БД так же, как и выбор живого игрока.
     *
     * Режим работы зависит от финансового состояния системы (protectionMode):
     *   - protectionMode=false: случайный выбор (SecureRandom shuffle).
     *   - protectionMode=true : боты выбирают бочки с наибольшими весами,
     *     чтобы увеличить свои шансы на победу и снизить потери системы.
     *
     * protectionMode включается в RoundLifecycleService, если накопленный баланс системы отрицательный.
     */
    @Override
    @Transactional
    public void submitBotSelections(UUID roomId, int roundNumber, boolean protectionMode,
                                    Map<UUID, BigDecimal> barrelWeights) {
        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        GameRoomConfig config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));

        ParticipantStatus botStatus = roundNumber == RoundConstants.ROUND_2 ? ParticipantStatus.FINALIST : ParticipantStatus.ACTIVE;
        List<GameParticipant> bots = participantRepository.list(
                GameParticipantQuery.byRoomAndStatus(roomId, botStatus))
                .stream().filter(GameParticipant::isBot).toList();

        if (bots.isEmpty()) return;

        log.info("Submitting bot selections for room {} round {} protectionMode={} bots={}",
                roomId, roundNumber, protectionMode, bots.size());

        for (GameParticipant bot : bots) {
            List<Barrel> orderedBarrels = selectBarrels(barrels, barrelWeights, config.getMaxBarrelSelection(),
                    protectionMode);

            ParticipantRoundEntry entry = new ParticipantRoundEntry(roundResult.getId(), bot.getId());
            entry.setSelectionCount(orderedBarrels.size());
            entry.setSelectionTimestamp(Instant.now());
            entry = entryRepository.create(entry);

            List<ParticipantBarrelSelection> selections = new ArrayList<>();
            for (Barrel barrel : orderedBarrels) {
                selections.add(new ParticipantBarrelSelection(entry.getId(), barrel.getId()));
            }
            selectionRepository.createAll(selections);
        }
    }

    /**
     * Выбирает бочки для бота.
     *
     * protectionMode=true  → сортировка по убыванию веса, берём top-N.
     *                         Бот выбирает «лучшие» бочки, чтобы выиграть у реального игрока.
     * protectionMode=false → случайный порядок через SecureRandom, берём первые N.
     *                         Бот не имеет преимущества перед реальным игроком.
     */
    private List<Barrel> selectBarrels(List<Barrel> barrels, Map<UUID, BigDecimal> barrelWeights,
                                       int maxSelection, boolean protectionMode) {
        int count = Math.min(maxSelection, barrels.size());
        if (protectionMode && barrelWeights != null && !barrelWeights.isEmpty()) {
            return barrels.stream()
                    .sorted(Comparator.comparing(
                            barrel -> barrelWeights.getOrDefault(barrel.getId(), BigDecimal.ZERO),
                            Comparator.reverseOrder()))
                    .limit(count)
                    .toList();
        }
        List<Barrel> shuffled = new ArrayList<>(barrels);
        Collections.shuffle(shuffled, rng);
        return shuffled.subList(0, count);
    }
}
