package com.vsrna.game.application.round;

import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.domain.exception.ApiException;
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
    private final BalanceCompensationHelper balanceCompensationHelper;
    private final BalancePort balancePort;

    @Transactional
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        var config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (!config.isBoostEnabled()) {
            throw ApiException.badRequest("Boost is not enabled in this room");
        }

        // Проверка баланса до транзакции
        BigDecimal available = balancePort.getAvailableBalance(userId);
        if (available.compareTo(config.getBoostCostAmount()) < 0) {
            throw ApiException.insufficientBalance(
                    "Недостаточно баллов для покупки буста. Требуется: " + config.getBoostCostAmount(),
                    Map.of("required", config.getBoostCostAmount(), "available", available));
        }

        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedRoundStatus = roundNumber == RoundConstants.ROUND_1
                ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        if (room.getStatus() != expectedRoundStatus) {
            throw ApiException.badRequest("Boost can only be purchased during the active round");
        }

        var participant = participantRepository.get(GameParticipantQuery.byRoomAndUser(roomId, userId));

        if (roundNumber == RoundConstants.ROUND_2) {
            var round1Result = roundResultRepository.find(RoundResultQuery.byRoomAndRound(roomId, RoundConstants.ROUND_1));
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

}
