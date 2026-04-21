package com.vsrna.game.application.round;

import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.domain.participant.GameParticipant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class BalanceCompensationHelper {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final BalancePort balancePort;
    private final Executor compensationExecutor;

    public BalanceCompensationHelper(BalancePort balancePort,
                                     @Qualifier("compensationExecutor") Executor compensationExecutor) {
        this.balancePort = balancePort;
        this.compensationExecutor = compensationExecutor;
    }

    public void scheduleRelease(List<GameParticipant> participants, UUID roomId) {
        if (participants.isEmpty()) return;
        List<GameParticipant> snapshot = List.copyOf(participants);
        registerAfterCommit(() -> {
            for (GameParticipant participant : snapshot) {
                withRetry(
                        () -> balancePort.release(participant.getUserId(), participant.getReservedPoints(), roomId),
                        "release userId=" + participant.getUserId() + " roomId=" + roomId
                );
            }
        });
    }

    public void scheduleAward(UUID userId, BigDecimal amount, UUID roomId) {
        registerAfterCommit(() -> withRetry(
                () -> balancePort.award(userId, amount, roomId),
                "award userId=" + userId + " amount=" + amount + " roomId=" + roomId
        ));
    }

    public void scheduleDeduct(UUID userId, BigDecimal amount, UUID roomId) {
        registerAfterCommit(() -> withRetry(
                () -> balancePort.deduct(userId, amount, roomId),
                "deduct userId=" + userId + " roomId=" + roomId
        ));
    }

    // Submits work to a dedicated thread pool after the transaction commits,
    // so retry sleeps never block the Tomcat request thread.
    private void registerAfterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                compensationExecutor.execute(task);
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
