package com.vsrna.game.application.bot;

import com.vsrna.game.application.round.scoring.RoundConstants;
import com.vsrna.game.domain.barrel.*;
import com.vsrna.game.domain.gameroom.GameRoomConfig;
import com.vsrna.game.domain.gameroom.GameRoomConfigQuery;
import com.vsrna.game.domain.gameroom.GameRoomConfigRepository;
import com.vsrna.game.domain.participant.*;
import com.vsrna.game.domain.round.*;
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
            GameParticipant bot = new GameParticipant(roomId, null, true,
                    String.format(BOT_NAME_FORMAT, botNumber), entryFeeAmount);
            bots.add(participantRepository.create(bot));
        }
        return bots;
    }

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
