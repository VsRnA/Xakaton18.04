package com.vsrna.game.application.round;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.GameRoomConfigQuery;
import com.vsrna.game.domain.gameroom.GameRoomConfigRepository;
import com.vsrna.game.domain.gameroom.GameRoomPatch;
import com.vsrna.game.domain.gameroom.GameRoomQuery;
import com.vsrna.game.domain.gameroom.GameRoomRepository;
import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.domain.participant.GameParticipantQuery;
import com.vsrna.game.domain.participant.GameParticipantRepository;
import com.vsrna.game.domain.round.ParticipantBarrelSelectionQuery;
import com.vsrna.game.domain.round.ParticipantBarrelSelectionRepository;
import com.vsrna.game.domain.round.ParticipantRoundEntry;
import com.vsrna.game.domain.round.ParticipantRoundEntryPatch;
import com.vsrna.game.domain.round.ParticipantRoundEntryQuery;
import com.vsrna.game.domain.round.ParticipantRoundEntryRepository;
import com.vsrna.game.domain.round.RoundResultQuery;
import com.vsrna.game.domain.round.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoostService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final ParticipantBarrelSelectionRepository selectionRepository;
    private final BalanceCompensationHelper balanceCompensationHelper;

    @Transactional
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        var config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (!config.isBoostEnabled()) {
            throw ApiException.badRequest("Boost is not enabled in this room");
        }

        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedDecisionStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_DECISION_1 : GameRoomStatus.BOOST_DECISION_2;
        if (room.getStatus() != expectedDecisionStatus) {
            throw ApiException.badRequest("Boost can only be purchased during the boost decision window");
        }

        var participant = participantRepository.get(GameParticipantQuery.byRoomAndUser(roomId, userId));

        if (roundNumber == 2) {
            var round1Result = roundResultRepository.find(RoundResultQuery.byRoomAndRound(roomId, 1));
            if (round1Result.isPresent()) {
                var round1Entry = entryRepository.find(
                        ParticipantRoundEntryQuery.byRoundResultAndParticipant(round1Result.get().getId(), participant.getId()));
                if (round1Entry.isPresent() && round1Entry.get().isBoostPurchased()) {
                    throw ApiException.badRequest("Boost already used in round 1 — only one boost allowed per game");
                }
            }
        }

        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                GameRoomPatch.prizePool(room.getPrizePoolAmount().add(config.getBoostCostAmount())));

        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));

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

        balanceCompensationHelper.scheduleDeduct(userId, config.getBoostCostAmount(), roomId);
    }

    @Transactional
    public void applyBoost(UUID roomId, UUID userId, int roundNumber, UUID boostedBarrelId) {
        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedBoostStatus = roundNumber == 1
                ? GameRoomStatus.BOOST_WINDOW_1 : GameRoomStatus.BOOST_WINDOW_2;
        if (room.getStatus() != expectedBoostStatus) {
            throw ApiException.badRequest("Not in boost window for round " + roundNumber);
        }

        var participant = participantRepository.get(GameParticipantQuery.byRoomAndUser(roomId, userId));
        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        var entry = entryRepository.get(ParticipantRoundEntryQuery.byRoundResultAndParticipant(
                roundResult.getId(), participant.getId()));

        if (!entry.isBoostPurchased()) {
            throw ApiException.badRequest("Boost was not purchased for this round");
        }

        var selections = selectionRepository.list(ParticipantBarrelSelectionQuery.byEntry(entry.getId()));
        boolean found = selections.stream().anyMatch(s -> s.getBarrelId().equals(boostedBarrelId));
        if (!found) {
            throw ApiException.badRequest("Barrel not in your selection");
        }

        entryRepository.update(
                ParticipantRoundEntryQuery.byId(entry.getId()),
                ParticipantRoundEntryPatch.applyBoost(boostedBarrelId));
    }
}
