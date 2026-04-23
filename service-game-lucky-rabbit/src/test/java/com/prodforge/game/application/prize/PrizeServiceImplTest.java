package com.prodforge.game.application.prize;

import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.metrics.GameMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.application.port.GameNotifierPort;
import com.prodforge.game.domain.gameroom.GameRoomConfigRepository;
import com.prodforge.game.domain.gameroom.GameRoomRepository;
import com.prodforge.game.domain.history.GameHistoryRepository;
import com.prodforge.game.domain.participant.GameParticipant;
import com.prodforge.game.domain.participant.GameParticipantRepository;
import com.prodforge.game.domain.round.ParticipantRoundEntryRepository;
import com.prodforge.game.domain.round.RoundResultRepository;
import com.prodforge.game.fixtures.GameParticipantFixtures;
import com.prodforge.game.fixtures.GameRoomConfigFixtures;
import com.prodforge.game.fixtures.GameRoomFixtures;
import com.prodforge.game.fixtures.RoundEntryFixtures;
import com.prodforge.game.fixtures.RoundResultFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrizeServiceImplTest {

    @Mock GameRoomRepository gameRoomRepository;
    @Mock GameRoomConfigRepository gameRoomConfigRepository;
    @Mock GameParticipantRepository participantRepository;
    @Mock RoundResultRepository roundResultRepository;
    @Mock ParticipantRoundEntryRepository entryRepository;
    @Mock GameHistoryRepository gameHistoryRepository;
    @Mock GameEventPort gameEventPort;
    @Mock GameNotifierPort notifierPort;
    @Mock GameEventLogService gameEventLogService;
    @Spy GameMetrics gameMetrics = new GameMetrics(new SimpleMeterRegistry());

    @InjectMocks
    PrizeServiceImpl prizeService;

    private final UUID roomId = UUID.randomUUID();

    @Test
    void realWinner_prizeIsCorrectPercentage() {
        // prizePool = 1000, payoutPct = 90% → приз = 900
        UUID winnerId = UUID.randomUUID();
        var winner = GameParticipantFixtures.finalist(roomId, winnerId);
        var round2 = RoundResultFixtures.roundResult(roomId, 2);
        var winnerEntry = RoundEntryFixtures.rankedEntry(round2.getId(), winner.getId(), 1, new BigDecimal("10"));

        setupMocks(round2, winner, winnerEntry, List.of(winner));

        prizeService.distributePrize(roomId);

        ArgumentCaptor<BigDecimal> awardCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(gameEventPort).publishBalanceAward(eq(winnerId), awardCaptor.capture(), eq(roomId));
        assertThat(awardCaptor.getValue()).isEqualByComparingTo(new BigDecimal("900.00"));
    }

    @Test
    void botWinner_noPrizePublished() {
        var botWinner = GameParticipantFixtures.botParticipant(roomId);
        var round2 = RoundResultFixtures.roundResult(roomId, 2);
        var winnerEntry = RoundEntryFixtures.rankedEntry(round2.getId(), botWinner.getId(), 1, new BigDecimal("10"));

        setupMocks(round2, botWinner, winnerEntry, List.of(botWinner));

        prizeService.distributePrize(roomId);

        verify(gameEventPort, never()).publishBalanceAward(any(), any(), any());
    }

    @Test
    void losingFinalists_releasePublished() {
        UUID winnerId = UUID.randomUUID();
        UUID loserId  = UUID.randomUUID();
        var winner = GameParticipantFixtures.finalist(roomId, winnerId);
        var loser  = GameParticipantFixtures.finalist(roomId, loserId);
        var round2 = RoundResultFixtures.roundResult(roomId, 2);
        var winnerEntry = RoundEntryFixtures.rankedEntry(round2.getId(), winner.getId(), 1, new BigDecimal("10"));

        setupMocks(round2, winner, winnerEntry, List.of(winner, loser));

        prizeService.distributePrize(roomId);

        verify(gameEventPort).publishBalanceDeductReserved(eq(loserId), eq(loser.getReservedPoints()), eq(roomId));
        verify(gameEventPort, never()).publishBalanceRelease(eq(winnerId), any(), any());
    }

    private void setupMocks(com.prodforge.game.domain.round.RoundResult round2,
                            GameParticipant winner,
                            com.prodforge.game.domain.round.ParticipantRoundEntry winnerEntry,
                            List<GameParticipant> finalists) {
        var round1 = RoundResultFixtures.roundResult(roomId, 1);

        when(gameRoomRepository.get(any())).thenReturn(GameRoomFixtures.waitingRoomWith(roomId, 4, new BigDecimal("1000")));
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId)); // payoutPct = 90%
        when(roundResultRepository.get(any())).thenReturn(round2).thenReturn(round1);
        when(entryRepository.list(any())).thenReturn(List.of(winnerEntry));
        when(participantRepository.get(any())).thenReturn(winner);
        when(participantRepository.list(any())).thenReturn(finalists);
        when(gameRoomRepository.update(any(), any())).thenReturn(GameRoomFixtures.waitingRoomWith(roomId, 4, new BigDecimal("1000")));
        when(gameHistoryRepository.create(any())).thenAnswer(inv -> inv.getArgument(0));
    }
}
