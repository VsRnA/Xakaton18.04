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

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final BalancePort balancePort;

    public void scheduleRelease(List<GameParticipant> participants, UUID roomId) {
        if (participants.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (GameParticipant participant : participants) {
                    withRetry(
                        () -> balancePort.release(participant.getUserId(), participant.getReservedPoints(), roomId),
                        "release userId=" + participant.getUserId() + " roomId=" + roomId
                    );
                }
            }
        });
    }

    public void scheduleAward(UUID userId, BigDecimal amount, UUID roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                withRetry(
                    () -> balancePort.award(userId, amount, roomId),
                    "award userId=" + userId + " amount=" + amount + " roomId=" + roomId
                );
            }
        });
    }

    public void scheduleDeduct(UUID userId, BigDecimal amount, UUID roomId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                withRetry(
                    () -> balancePort.deduct(userId, amount, roomId),
                    "deduct userId=" + userId + " roomId=" + roomId
                );
            }
        });
    }

    private void withRetry(Runnable action, String description) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("COMPENSATION NEEDED [attempt {}/{}]: {}: {}",
                            attempt, MAX_ATTEMPTS, description, e.getMessage());
                } else {
                    log.warn("Balance operation failed [attempt {}/{}], retrying in {}ms: {}",
                            attempt, MAX_ATTEMPTS, RETRY_DELAY_MS * attempt, description);
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("COMPENSATION NEEDED (interrupted): {}", description);
                        return;
                    }
                }
            }
        }
    }
}
