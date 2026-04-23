package com.vsrna.game.application.gameroom;

import com.vsrna.game.application.bot.BotService;
import com.vsrna.game.application.gameevent.GameEventLogService;
import com.vsrna.game.application.metrics.GameMetrics;
import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.application.port.GameNotifierPort;
import com.vsrna.game.application.port.GameSchedulerPort;
import com.vsrna.game.application.gameroom.config.GameRoomConfigValidator;
import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.domain.barrel.BarrelRepository;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.gameroom.GameRoomConfigRepository;
import com.vsrna.game.domain.gameroom.GameRoomRepository;
import com.vsrna.game.domain.gameroom.GameRoomStatus;
import com.vsrna.game.domain.participant.GameParticipantRepository;
import com.vsrna.game.fixtures.GameRoomConfigFixtures;
import com.vsrna.game.fixtures.GameRoomFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameRoomServiceImplTest {

    @Mock GameRoomRepository gameRoomRepository;
    @Mock GameRoomConfigRepository gameRoomConfigRepository;
    @Mock GameParticipantRepository participantRepository;
    @Mock BarrelRepository barrelRepository;
    @Mock BalancePort balancePort;
    @Mock GameEventPort gameEventPort;
    @Mock BotService botService;
    @Mock GameSchedulerPort schedulerPort;
    @Mock GameNotifierPort notifierPort;
    @Mock RoundService roundService;
    @Mock GameRoomConfigValidator configValidator;
    @Mock GameEventLogService gameEventLogService;
    @Mock GameMetrics gameMetrics;

    @InjectMocks
    GameRoomServiceImpl gameRoomService;

    private final UUID roomId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void joinRoom_insufficientBalance_throws() {
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("50"));
        when(gameRoomRepository.getForUpdate(any())).thenReturn(GameRoomFixtures.waitingRoom(roomId));
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId)); // entryFee = 100
        when(gameRoomRepository.list(any())).thenReturn(List.of());

        assertThatThrownBy(() -> gameRoomService.joinRoom(roomId, userId, "Player"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void joinRoom_roomFull_throws() {
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("500"));
        when(gameRoomRepository.getForUpdate(any())).thenReturn(GameRoomFixtures.waitingRoomWith(roomId, 4, new BigDecimal("400")));
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.configWith(roomId, 4, new BigDecimal("100")));

        assertThatThrownBy(() -> gameRoomService.joinRoom(roomId, userId, "Player"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Room is full");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void joinRoom_roomNotWaiting_throws() {
        var room = GameRoomFixtures.waitingRoom(roomId);
        room.setStatus(GameRoomStatus.ROUND_1);

        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("500"));
        when(gameRoomRepository.getForUpdate(any())).thenReturn(room);
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId));

        assertThatThrownBy(() -> gameRoomService.joinRoom(roomId, userId, "Player"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not accepting");

        verifyNoInteractions(gameEventPort);
    }

    @Test
    void joinRoom_duplicateJoin_noReservePublished() {
        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("500"));
        when(gameRoomRepository.getForUpdate(any())).thenReturn(GameRoomFixtures.waitingRoom(roomId));
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId));
        when(participantRepository.create(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> gameRoomService.joinRoom(roomId, userId, "Player"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already joined");

        // Критично: reserve-команда НЕ должна попасть в outbox при дубле
        verify(gameEventPort, never()).publishBalanceReserve(any(), any(), any());
    }

    @Test
    void joinRoom_success_publishesReserveAndEntryReserved() {
        var updatedRoom = GameRoomFixtures.waitingRoomWith(roomId, 1, new BigDecimal("100"));

        when(balancePort.getAvailableBalance(userId)).thenReturn(new BigDecimal("500"));
        when(gameRoomRepository.getForUpdate(any())).thenReturn(GameRoomFixtures.waitingRoom(roomId));
        when(gameRoomConfigRepository.get(any())).thenReturn(GameRoomConfigFixtures.config(roomId)); // entryFee = 100
        when(participantRepository.create(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gameRoomRepository.update(any(), any())).thenReturn(updatedRoom);
        when(schedulerPort.scheduleWaitTimerExpiry(roomId)).thenReturn(null);

        gameRoomService.joinRoom(roomId, userId, "Player");

        verify(gameEventPort).publishBalanceReserve(userId, new BigDecimal("100"), roomId);
        verify(gameEventPort).publishEntryReserved(userId, roomId, new BigDecimal("100"));
    }
}
