package com.vsrna.game.application.bot;

import com.vsrna.game.domain.barrel.*;
import com.vsrna.game.domain.participant.*;
import com.vsrna.game.domain.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final ParticipantBarrelSelectionRepository selectionRepository;

    private final SecureRandom rng = new SecureRandom();

    @Override
    @Transactional
    public List<GameParticipant> createBotsForRoom(UUID roomId, int count, BigDecimal entryFeeAmount) {
        List<GameParticipant> bots = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            GameParticipant bot = new GameParticipant(roomId, null, true, entryFeeAmount);
            bots.add(participantRepository.create(bot));
        }
        return bots;
    }

    @Override
    @Transactional
    public void submitBotSelections(UUID roomId, int roundNumber) {
        RoundResult roundResult = roundResultRepository.get(
                RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        List<Barrel> barrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        List<GameParticipant> bots = participantRepository.list(
                GameParticipantQuery.byRoomAndStatus(roomId, ParticipantStatus.ACTIVE))
                .stream().filter(GameParticipant::isBot).toList();

        for (GameParticipant bot : bots) {
            List<Barrel> shuffled = new ArrayList<>(barrels);
            Collections.shuffle(shuffled, rng);
            int selectionCount = 1 + rng.nextInt(5);
            List<Barrel> selected = shuffled.subList(0, Math.min(selectionCount, shuffled.size()));

            ParticipantRoundEntry entry = new ParticipantRoundEntry(roundResult.getId(), bot.getId());
            entry.setSelectionCount(selected.size());
            entry.setSelectionTimestamp(Instant.now());
            entry = entryRepository.create(entry);

            List<ParticipantBarrelSelection> selections = new ArrayList<>();
            for (Barrel barrel : selected) {
                selections.add(new ParticipantBarrelSelection(entry.getId(), barrel.getId()));
            }
            selectionRepository.createAll(selections);
        }
    }
}
