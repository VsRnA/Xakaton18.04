package com.prodforge.game.application.round;

import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.metrics.GameMetrics;
import com.prodforge.game.application.round.scoring.RoundConstants;
import com.prodforge.game.application.port.BalancePort;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.exception.GameErrorMessages;
import com.prodforge.game.domain.gameroom.GameRoomConfigQuery;
import com.prodforge.game.domain.gameroom.GameRoomConfigRepository;
import com.prodforge.game.domain.gameroom.GameRoomQuery;
import com.prodforge.game.domain.gameroom.GameRoomRepository;
import com.prodforge.game.domain.gameroom.GameRoomStatus;
import com.prodforge.game.domain.participant.GameParticipantQuery;
import com.prodforge.game.domain.participant.GameParticipantRepository;
import com.prodforge.game.domain.round.ParticipantRoundEntry;
import com.prodforge.game.domain.round.ParticipantRoundEntryPatch;
import com.prodforge.game.domain.round.ParticipantRoundEntryQuery;
import com.prodforge.game.domain.round.ParticipantRoundEntryRepository;
import com.prodforge.game.domain.round.RoundResultQuery;
import com.prodforge.game.domain.round.RoundResultRepository;
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
    private final GameMetrics gameMetrics;

    /**
     * Покупка буста игроком в текущем раунде.
     *
     * Буст — это платное улучшение, которое при подсчёте очков изменяет вес одной бочки:
     *   - Самый отрицательный вес → инвертируется (становится положительным).
     *   - Если отрицательных нет → наименьший положительный удваивается.
     * Фактический эффект рассчитывается в RoundScoringUtils.computeBoostEffect().
     *
     * Ограничения:
     *   - Буст можно купить только пока раунд активен (статус ROUND_1 / ROUND_2).
     *   - Один буст на раунд: повторная покупка в том же раунде запрещена.
     *   - Один буст на всю игру: если уже использован в раунде 1, в раунде 2 недоступен.
     *   - Баланс игрока должен покрывать стоимость буста.
     *
     * Деньги списываются через BalancePort (событие в Kafka, не прямой вызов).
     */
    @Transactional
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        var config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (!config.isBoostEnabled()) {
            throw ApiException.badRequest(GameErrorMessages.BOOST_NOT_ENABLED);
        }

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

        // Проверка: буст уже куплен в этом раунде
        var existingEntry = entryRepository.find(
                ParticipantRoundEntryQuery.byRoundResultAndParticipant(roundResult.getId(), participant.getId()));
        if (existingEntry.isPresent() && existingEntry.get().isBoostPurchased()) {
            throw ApiException.badRequest(GameErrorMessages.BOOST_ALREADY_PURCHASED_THIS_ROUND);
        }

        // Проверка: буст уже использован в раунде 1 (запрос на раунд 2)
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

        // Флаг буста проставляется в запись участника — фактический пересчёт при финализации раунда
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

        gameEventPort.publishBalanceDeduct(userId, config.getBoostCostAmount(), roomId);
        gameMetrics.boostsPurchased.increment();
        gameEventLogService.log(roomId, "BOOST_PURCHASED", Map.of(
                "userId", userId.toString(),
                "round", roundNumber,
                "cost", config.getBoostCostAmount()
        ));
    }
}
