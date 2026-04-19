package com.vsrna.game.application.port;

import java.util.UUID;

/**
 * Порт планировщика игровых событий.
 * Application layer не зависит от Quartz напрямую.
 */
public interface GameSchedulerPort {
    void scheduleWaitTimerExpiry(UUID roomId);
    void scheduleRoundEnd(UUID roomId, int roundNumber);
    void scheduleBoostDecisionEnd(UUID roomId, int roundNumber);
    void scheduleBoostWindowEnd(UUID roomId, int roundNumber);
    void cancel(UUID roomId, String phase);
}
