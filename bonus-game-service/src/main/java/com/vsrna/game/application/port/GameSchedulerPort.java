package com.vsrna.game.application.port;

import java.time.Instant;
import java.util.UUID;

public interface GameSchedulerPort {
    Instant scheduleWaitTimerExpiry(UUID roomId);
    void scheduleRoundEnd(UUID roomId, int roundNumber);
    void scheduleBoostDecisionEnd(UUID roomId, int roundNumber);
    void scheduleBoostWindowEnd(UUID roomId, int roundNumber);
    void scheduleRoomOpen(UUID roomId, Instant startAt);
    void cancel(UUID roomId, String phase);
}
