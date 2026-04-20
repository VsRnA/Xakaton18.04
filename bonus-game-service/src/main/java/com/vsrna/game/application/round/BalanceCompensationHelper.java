package com.vsrna.game.application.round;

import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.domain.participant.GameParticipant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalanceCompensationHelper {

    private final BalancePort balancePort;

    public void scheduleRelease(List<GameParticipant> participants, UUID roomId) {
        if (participants.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (GameParticipant participant : participants) {
                    try {
                        balancePort.release(participant.getUserId(), participant.getReservedPoints(), roomId);
                    } catch (Exception e) {
                        log.error("COMPENSATION NEEDED: failed to release balance userId={}, roomId={}: {}",
                                participant.getUserId(), roomId, e.getMessage());
                    }
                }
            }
        });
    }

    public void scheduleDeduct(UUID userId, BigDecimal amount, UUID roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    balancePort.deduct(userId, amount, roomId);
                } catch (Exception e) {
                    log.error("COMPENSATION NEEDED: failed to deduct balance userId={}, roomId={}: {}",
                            userId, roomId, e.getMessage());
                }
            }
        });
    }
}
