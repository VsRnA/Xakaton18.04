package com.vsrna.backend.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GameScheduler {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    /**
     * Таймер ожидания игроков — 60с, затем заполнение ботами.
     */
    public void scheduleWaitTimerExpiry(UUID roomId, Runnable task) {
        schedule(roomId, "wait", task, 60, TimeUnit.SECONDS);
    }

    /**
     * Завершение раунда — 30с, затем resolveRound.
     */
    public void scheduleRoundEnd(UUID roomId, int roundNumber, Runnable task) {
        schedule(roomId, "round-" + roundNumber, task, 30, TimeUnit.SECONDS);
    }

    /**
     * Окно буста — 5с, затем finalizeRound.
     */
    public void scheduleBoostWindowEnd(UUID roomId, int roundNumber, Runnable task) {
        schedule(roomId, "boost-" + roundNumber, task, 5, TimeUnit.SECONDS);
    }

    /**
     * Отмена задачи по roomId + phase.
     */
    public void cancel(UUID roomId, String phase) {
        String key = key(roomId, phase);
        ScheduledFuture<?> future = pendingTasks.remove(key);
        if (future != null) {
            future.cancel(false);
            log.debug("Cancelled scheduler task: {}", key);
        }
    }

    private void schedule(UUID roomId, String phase, Runnable task, long delay, TimeUnit unit) {
        String key = key(roomId, phase);
        cancel(roomId, phase);
        ScheduledFuture<?> future = executor.schedule(() -> {
            pendingTasks.remove(key);
            try {
                task.run();
            } catch (Exception e) {
                log.error("Scheduled task failed [{}/{}]: {}", roomId, phase, e.getMessage(), e);
            }
        }, delay, unit);
        pendingTasks.put(key, future);
        log.debug("Scheduled task: {} in {}s", key, delay);
    }

    private String key(UUID roomId, String phase) {
        return roomId + "/" + phase;
    }
}
