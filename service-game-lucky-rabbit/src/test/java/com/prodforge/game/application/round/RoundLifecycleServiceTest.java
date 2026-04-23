package com.prodforge.game.application.round;

import com.prodforge.game.application.bot.BotService;
import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.metrics.GameMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.prodforge.game.application.port.GameEventPort;
import com.prodforge.game.application.port.GameNotifierPort;
import com.prodforge.game.application.port.GameSchedulerPort;
import com.prodforge.game.application.prize.PrizeService;
import com.prodforge.game.domain.barrel.BarrelRepository;
import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.gameroom.GameRoom;
import com.prodforge.game.domain.gameroom.GameRoomPatch;
import com.prodforge.game.domain.gameroom.GameRoomRepository;
import com.prodforge.game.domain.gameroom.GameRoomStatus;
import com.prodforge.game.domain.history.GameHistoryAnalyticsRepository;
import com.prodforge.game.domain.participant.GameParticipant;
import com.prodforge.game.domain.participant.GameParticipantPatch;
import com.prodforge.game.domain.participant.GameParticipantRepository;
import com.prodforge.game.domain.participant.ParticipantStatus;
import com.prodforge.game.domain.rng.RngPort;
import com.prodforge.game.domain.round.ParticipantBarrelSelectionRepository;
import com.prodforge.game.domain.round.ParticipantRoundEntryRepository;
import com.prodforge.game.domain.round.RoundResultRepository;
import com.prodforge.game.fixtures.GameParticipantFixtures;
import com.prodforge.game.fixtures.GameRoomFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundLifecycleServiceTest {

    @Mock GameRoomRepository gameRoomRepository;
    @Mock GameParticipantRepository participantRepository;
    @Mock BarrelRepository barrelRepository;
    @Mock RoundResultRepository roundResultRepository;
    @Mock ParticipantRoundEntryRepository entryRepository;
    @Mock ParticipantBarrelSelectionRepository selectionRepository;
    @Mock RngPort rngPort;
    @Mock GameSchedulerPort schedulerPort;
    @Mock GameNotifierPort notifierPort;
    @Mock PrizeService prizeService;
    @Mock GameHistoryAnalyticsRepository gameHistoryAnalyticsRepository;
    @Mock BotService botService;
    @Mock GameEventPort gameEventPort;
    @Mock GameEventLogService gameEventLogService;
    @Spy GameMetrics gameMetrics = new GameMetrics(new SimpleMeterRegistry());

    @InjectMocks
    RoundLifecycleService service;

    private final UUID roomId = UUID.randomUUID();
    private final UUID userId1 = UUID.randomUUID();
    private final UUID userId2 = UUID.randomUUID();

    // ── markFinalistReady ────────────────────────────────────────────────────

    @Test
    void markFinalistReady_wrongStatus_throws() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.ROUND_1);
        when(gameRoomRepository.get(any())).thenReturn(room);

        assertThatThrownBy(() -> service.markFinalistReady(roomId, userId1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("finalists-ready phase");
    }

    @Test
    void markFinalistReady_notFinalist_throws() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.WAITING_FINALISTS_READY);
        when(gameRoomRepository.get(any())).thenReturn(room);

        GameParticipant active = GameParticipantFixtures.realParticipant(roomId, userId1);
        active.setStatus(ParticipantStatus.ACTIVE);
        when(participantRepository.get(any())).thenReturn(active);

        assertThatThrownBy(() -> service.markFinalistReady(roomId, userId1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only finalists");
    }

    @Test
    void markFinalistReady_alreadyReady_idempotent() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.WAITING_FINALISTS_READY);
        when(gameRoomRepository.get(any())).thenReturn(room);

        GameParticipant finalist = GameParticipantFixtures.finalist(roomId, userId1);
        finalist.setRound2Ready(true);
        when(participantRepository.get(any())).thenReturn(finalist);

        service.markFinalistReady(roomId, userId1);

        verify(participantRepository, never()).update(any(), eq(GameParticipantPatch.markRound2Ready()));
    }

    @Test
    void markFinalistReady_firstOfTwo_doesNotStartRound2() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.WAITING_FINALISTS_READY);
        when(gameRoomRepository.get(any())).thenReturn(room);

        GameParticipant p1 = GameParticipantFixtures.finalist(roomId, userId1);
        when(participantRepository.get(any())).thenReturn(p1);

        // After marking p1 ready, list returns p1 (round2Ready=false still in mock)
        // and p2 (not ready) — so not all ready
        GameParticipant p1Updated = GameParticipantFixtures.finalist(roomId, userId1);
        p1Updated.setRound2Ready(true);
        GameParticipant p2 = GameParticipantFixtures.finalist(roomId, userId2);
        p2.setRound2Ready(false);

        when(participantRepository.update(any(), eq(GameParticipantPatch.markRound2Ready()))).thenReturn(p1Updated);
        when(participantRepository.list(any())).thenReturn(List.of(p1Updated, p2));

        service.markFinalistReady(roomId, userId1);

        verify(schedulerPort, never()).cancel(any(), any());
        verify(barrelRepository, never()).list(any()); // startRound not called
    }

    @Test
    void markFinalistReady_bothReady_cancelsTimeoutAndStartsRound2() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.WAITING_FINALISTS_READY);
        when(gameRoomRepository.get(any())).thenReturn(room);

        GameParticipant p1 = GameParticipantFixtures.finalist(roomId, userId1);
        when(participantRepository.get(any())).thenReturn(p1);

        GameParticipant p1Updated = GameParticipantFixtures.finalist(roomId, userId1);
        p1Updated.setRound2Ready(true);
        GameParticipant p2 = GameParticipantFixtures.finalist(roomId, userId2);
        p2.setRound2Ready(true); // already ready

        when(participantRepository.update(any(), eq(GameParticipantPatch.markRound2Ready()))).thenReturn(p1Updated);
        when(participantRepository.list(any())).thenReturn(List.of(p1Updated, p2));

        // startRound needs these
        when(rngPort.commit(eq(roomId), eq(2))).thenReturn(new com.prodforge.game.domain.rng.RngCommitment("hash", "seed"));
        when(roundResultRepository.create(any())).thenAnswer(inv -> {
            var rr = (com.prodforge.game.domain.round.RoundResult) inv.getArgument(0);
            rr.setId(UUID.randomUUID());
            return rr;
        });
        when(roundResultRepository.update(any(), any())).thenReturn(null);
        when(barrelRepository.list(any())).thenReturn(List.of());
        when(gameRoomRepository.update(any(), any())).thenReturn(room);
        when(schedulerPort.scheduleRoundEnd(any(), anyInt())).thenReturn(Instant.now().plusSeconds(30));

        service.markFinalistReady(roomId, userId1);

        verify(schedulerPort).cancel(roomId, com.prodforge.game.application.port.GamePhase.START_ROUND2);
        verify(schedulerPort).scheduleRoundEnd(roomId, 2);
        verify(notifierPort).publishRoundEvent(eq(roomId), any());
    }

    // ── startRound2AfterTimeout ──────────────────────────────────────────────

    @Test
    void startRound2AfterTimeout_wrongStatus_skips() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.ROUND_2);
        when(gameRoomRepository.get(any())).thenReturn(room);

        service.startRound2AfterTimeout(roomId);

        verifyNoInteractions(schedulerPort, notifierPort);
    }

    @Test
    void startRound2AfterTimeout_inWaitingPhase_startsRound2() {
        GameRoom room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.WAITING_FINALISTS_READY);
        when(gameRoomRepository.get(any())).thenReturn(room);

        when(rngPort.commit(eq(roomId), eq(2))).thenReturn(new com.prodforge.game.domain.rng.RngCommitment("hash", "seed"));
        when(roundResultRepository.create(any())).thenAnswer(inv -> {
            var rr = (com.prodforge.game.domain.round.RoundResult) inv.getArgument(0);
            rr.setId(UUID.randomUUID());
            return rr;
        });
        when(roundResultRepository.update(any(), any())).thenReturn(null);
        when(barrelRepository.list(any())).thenReturn(List.of());
        when(gameRoomRepository.update(any(), any())).thenReturn(room);
        when(schedulerPort.scheduleRoundEnd(any(), anyInt())).thenReturn(Instant.now().plusSeconds(30));

        service.startRound2AfterTimeout(roomId);

        verify(schedulerPort).scheduleRoundEnd(roomId, 2);
        verify(notifierPort).publishRoundEvent(eq(roomId), any());
    }

    // ── advanceToFinal (via finalizeRound) — interaction tests ───────────────

    @Test
    void advanceToFinal_allBotFinalists_startsRound2Immediately() {
        // Two bot finalists — no waiting phase needed
        UUID botId1 = UUID.randomUUID();
        UUID botId2 = UUID.randomUUID();

        GameParticipant bot1 = GameParticipantFixtures.botParticipant(roomId);
        bot1.setId(botId1);
        GameParticipant bot2 = GameParticipantFixtures.botParticipant(roomId);
        bot2.setId(botId2);

        // advanceToFinal() calls update() twice for finalists, then markRound2Ready() for each bot
        when(participantRepository.update(any(), eq(GameParticipantPatch.advanceToFinal()))).thenReturn(bot1, bot2);
        when(participantRepository.update(any(), eq(GameParticipantPatch.markRound2Ready()))).thenReturn(bot1);

        // startRound needs
        when(rngPort.commit(eq(roomId), eq(2))).thenReturn(new com.prodforge.game.domain.rng.RngCommitment("hash", "seed"));
        when(roundResultRepository.create(any())).thenAnswer(inv -> {
            var rr = (com.prodforge.game.domain.round.RoundResult) inv.getArgument(0);
            rr.setId(UUID.randomUUID());
            return rr;
        });
        when(roundResultRepository.update(any(), any())).thenReturn(null);
        when(barrelRepository.list(any())).thenReturn(List.of());
        when(gameRoomRepository.update(any(), any())).thenReturn(GameRoomFixtures.waitingRoom(roomId));
        when(schedulerPort.scheduleRoundEnd(any(), anyInt())).thenReturn(Instant.now().plusSeconds(30));

        // Call advanceToFinal via the private method through finalizeRound would be complex;
        // test it via direct invocation using reflection or test the observable behaviour:
        // scheduleFinalistsReadyTimeout must NOT be called, scheduleRoundEnd(2) must BE called
        invokeAdvanceToFinal(List.of(
                entryFor(botId1), entryFor(botId2)
        ), "SCORE");

        verify(schedulerPort, never()).scheduleFinalistsReadyTimeout(any());
        verify(schedulerPort).scheduleRoundEnd(roomId, 2);
    }

    @Test
    void advanceToFinal_realPlayerFinalists_transitionsToWaitingPhase() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();

        GameParticipant real1 = GameParticipantFixtures.realParticipant(roomId, userId1);
        real1.setId(p1Id);
        real1.setStatus(ParticipantStatus.FINALIST);
        GameParticipant real2 = GameParticipantFixtures.realParticipant(roomId, userId2);
        real2.setId(p2Id);
        real2.setStatus(ParticipantStatus.FINALIST);

        when(participantRepository.update(any(), eq(GameParticipantPatch.advanceToFinal()))).thenReturn(real1, real2);
        when(gameRoomRepository.update(any(), any())).thenReturn(GameRoomFixtures.waitingRoom(roomId));

        invokeAdvanceToFinal(List.of(entryFor(p1Id), entryFor(p2Id)), "SCORE");

        verify(gameRoomRepository).update(any(), eq(GameRoomPatch.status(GameRoomStatus.WAITING_FINALISTS_READY)));
        verify(schedulerPort).scheduleFinalistsReadyTimeout(roomId);
        verify(schedulerPort, never()).scheduleRoundEnd(roomId, 2);
    }

    // Helper to invoke advanceToFinal indirectly without going through full finalizeRound
    private void invokeAdvanceToFinal(List<com.prodforge.game.domain.round.ParticipantRoundEntry> entries, String winCriteria) {
        try {
            var method = RoundLifecycleService.class.getDeclaredMethod(
                    "advanceToFinal", UUID.class, List.class, String.class);
            method.setAccessible(true);
            method.invoke(service, roomId, entries, winCriteria);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private com.prodforge.game.domain.round.ParticipantRoundEntry entryFor(UUID participantId) {
        UUID roundResultId = UUID.randomUUID();
        var e = new com.prodforge.game.domain.round.ParticipantRoundEntry(roundResultId, participantId);
        e.setId(UUID.randomUUID());
        e.setTotalScore(BigDecimal.ONE);
        e.setSelectionTimestamp(java.time.Instant.now());
        return e;
    }
}
