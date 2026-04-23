package com.prodforge.game.application.port;

import java.time.Instant;
import java.util.UUID;

public interface GameSchedulerPort {
    Instant scheduleWaitTimerExpiry(UUID roomId);
    Instant scheduleRoundEnd(UUID roomId, int roundNumber);
    Instant scheduleBoostDecisionEnd(UUID roomId, int roundNumber);
    Instant scheduleBoostWindowEnd(UUID roomId, int roundNumber);
    void scheduleFinalistsReadyTimeout(UUID roomId);
    void scheduleRoomOpen(UUID roomId, Instant startAt);
    void cancel(UUID roomId, GamePhase phase);
}
