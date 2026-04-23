package com.prodforge.game.application.round;

import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.metrics.GameMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.prodforge.game.application.port.BalancePort;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.gameroom.GameRoomStatus;
import com.prodforge.game.domain.round.ParticipantRoundEntryQuery;
import com.prodforge.game.domain.round.RoundResultQuery;
import com.prodforge.game.fixtures.GameParticipantFixtures;
import com.prodforge.game.fixtures.GameRoomConfigFixtures;
import com.prodforge.game.fixtures.GameRoomFixtures;
import com.prodforge.game.fixtures.RoundEntryFixtures;
import com.prodforge.game.fixtures.RoundResultFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.prodforge.game.domain.gameroom.GameRoomConfigRepository;
import com.prodforge.game.domain.gameroom.GameRoomRepository;
import com.prodforge.game.domain.participant.GameParticipantRepository;
import com.prodforge.game.domain.round.ParticipantRoundEntryRepository;
import com.prodforge.game.domain.round.RoundResultRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoostServiceTest {

    @Mock GameRoomRepository gameRoomRepository;
    @Mock GameRoomConfigRepository gameRoomConfigRepository;
    @Mock GameParticipantRepository participantRepository;
    @Mock RoundResultRepository roundResultRepository;
    @Mock ParticipantRoundEntryRepository entryRepository;
    @Mock BalancePort balancePort;
    @Mock GameEventPort gameEventPort;
    @Mock GameEventLogService gameEventLogService;
    @Spy GameMetrics gameMetrics = new GameMetrics(new SimpleMeterRegistry());

    @InjectMocks
    BoostService boostService;

    private final UUID roomId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void purchaseBoost_insufficientBalance_throws() {
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId));
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("10"));
        // boostCost = 50

        assertThatThrownBy(() -> boostService.purchaseBoost(roomId, userId, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Недостаточно");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void purchaseBoost_wrongRoomStatus_throws() {
        var room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.BOOST_DECISION_1); // не ROUND_1

        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId));
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("200"));
        when(gameRoomRepository.get(any())).thenReturn(room);

        assertThatThrownBy(() -> boostService.purchaseBoost(roomId, userId, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("активного раунда");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void purchaseBoost_alreadyPurchasedThisRound_throws() {
        var room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.ROUND_1);
        var participant = GameParticipantFixtures.realParticipant(roomId, userId);
        var roundResult = RoundResultFixtures.roundResult(roomId, 1);
        var existingEntry = RoundEntryFixtures.boostedEntry(roundResult.getId(), participant.getId());

        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId));
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("200"));
        when(gameRoomRepository.get(any())).thenReturn(room);
        when(participantRepository.get(any())).thenReturn(participant);
        when(roundResultRepository.get(any())).thenReturn(roundResult);
        when(entryRepository.find(any())).thenReturn(Optional.of(existingEntry));

        assertThatThrownBy(() -> boostService.purchaseBoost(roomId, userId, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("уже куплен");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void purchaseBoost_round2_usedBoostInRound1_throws() {
        var room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.ROUND_2);
        var participant = GameParticipantFixtures.realParticipant(roomId, userId);
        var round2 = RoundResultFixtures.roundResult(roomId, 2);
        var round1 = RoundResultFixtures.roundResult(roomId, 1);
        var round1Entry = RoundEntryFixtures.boostedEntry(round1.getId(), participant.getId());

        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId));
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("200"));
        when(gameRoomRepository.get(any())).thenReturn(room);
        when(participantRepository.get(any())).thenReturn(participant);
        when(roundResultRepository.get(any())).thenReturn(round2);
        when(entryRepository.find(ParticipantRoundEntryQuery.byRoundResultAndParticipant(
                round2.getId(), participant.getId()))).thenReturn(Optional.empty());
        when(roundResultRepository.find(RoundResultQuery.byRoomAndRound(roomId, 1)))
                .thenReturn(Optional.of(round1));
        when(entryRepository.find(ParticipantRoundEntryQuery.byRoundResultAndParticipant(
                round1.getId(), participant.getId()))).thenReturn(Optional.of(round1Entry));

        assertThatThrownBy(() -> boostService.purchaseBoost(roomId, userId, 2))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("уже использован в раунде 1");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void purchaseBoost_success_publishesDeductCommand() {
        var config = GameRoomConfigFixtures.config(roomId);
        var room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.ROUND_1);
        room.setPrizePoolAmount(new BigDecimal("400"));
        var participant = GameParticipantFixtures.realParticipant(roomId, userId);
        var roundResult = RoundResultFixtures.roundResult(roomId, 1);

        when(gameRoomConfigRepository.get(any())).thenReturn(config);
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("200"));
        when(gameRoomRepository.get(any())).thenReturn(room);
        when(participantRepository.get(any())).thenReturn(participant);
        when(roundResultRepository.get(any())).thenReturn(roundResult);
        when(entryRepository.find(any())).thenReturn(Optional.empty());

        boostService.purchaseBoost(roomId, userId, 1);

        verify(gameEventPort).publishBalanceDeduct(userId, config.getBoostCostAmount(), roomId);
    }
}
