package com.vsrna.game.application.round;

import com.vsrna.game.application.gameevent.GameEventLogService;
import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.exception.GameErrorMessages;
import com.vsrna.game.domain.gameroom.GameRoomConfigQuery;
import com.vsrna.game.domain.gameroom.GameRoomConfigRepository;
import com.vsrna.game.domain.gameroom.GameRoomPatch;
import com.vsrna.game.domain.gameroom.GameRoomQuery;
import com.vsrna.game.domain.gameroom.GameRoomRepository;
import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.domain.participant.GameParticipantQuery;
import com.vsrna.game.domain.participant.GameParticipantRepository;
import com.vsrna.game.domain.round.ParticipantRoundEntry;
import com.vsrna.game.domain.round.ParticipantRoundEntryPatch;
import com.vsrna.game.domain.round.ParticipantRoundEntryQuery;
import com.vsrna.game.domain.round.ParticipantRoundEntryRepository;
import com.vsrna.game.domain.round.RoundResultQuery;
import com.vsrna.game.domain.round.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoostService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final BalancePort balancePort;
    private final GameEventPort gameEventPort;
    private final GameEventLogService gameEventLogService;

    @Transactional
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        var config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (!config.isBoostEnabled()) {
            throw ApiException.badRequest(GameErrorMessages.BOOST_NOT_ENABLED);
        }

        // Check balance before any DB writes — fail fast if insufficient funds
        BigDecimal available = balancePort.getAvailableBalance(userId);
        if (available.compareTo(config.getBoostCostAmount()) < 0) {
            throw ApiException.insufficientBalance(
                    GameErrorMessages.insufficientBalanceForBoost(config.getBoostCostAmount()),
                    Map.of("required", config.getBoostCostAmount(), "available", available));
        }

        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedRoundStatus = roundNumber == RoundConstants.ROUND_1
                ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        if (room.getStatus() != expectedRoundStatus) {
            throw ApiException.badRequest(GameErrorMessages.BOOST_WRONG_ROUND_STATUS);
        }

        var participant = participantRepository.get(GameParticipantQuery.byRoomAndUser(roomId, userId));
        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        // Idempotency: prevent double-purchase in the current round
        var existingEntry = entryRepository.find(
                ParticipantRoundEntryQuery.byRoundResultAndParticipant(roundResult.getId(), participant.getId()));
        if (existingEntry.isPresent() && existingEntry.get().isBoostPurchased()) {
            throw ApiException.badRequest(GameErrorMessages.BOOST_ALREADY_PURCHASED_THIS_ROUND);
        }

        // Prevent boost in round 2 if already used in round 1
        if (roundNumber == RoundConstants.ROUND_2) {
            var round1Result = roundResultRepository.find(RoundResultQuery.byRoomAndRound(roomId, RoundConstants.ROUND_1));
            if (round1Result.isPresent()) {
                var round1Entry = entryRepository.find(
                        ParticipantRoundEntryQuery.byRoundResultAndParticipant(round1Result.get().getId(), participant.getId()));
                if (round1Entry.isPresent() && round1Entry.get().isBoostPurchased()) {
                    throw ApiException.badRequest(GameErrorMessages.BOOST_ALREADY_USED);
                }
            }
        }

        gameRoomRepository.update(GameRoomQuery.byId(roomId),
                GameRoomPatch.prizePool(room.getPrizePoolAmount().add(config.getBoostCostAmount())));

        existingEntry.ifPresentOrElse(
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

        // Write deduct command atomically with entry update — guaranteed delivery via outbox
        gameEventPort.publishBalanceDeduct(userId, config.getBoostCostAmount(), roomId);
        gameEventLogService.log(roomId, "BOOST_PURCHASED", Map.of(
                "userId", userId.toString(),
                "round", roundNumber,
                "cost", config.getBoostCostAmount()
        ));
    }
}
