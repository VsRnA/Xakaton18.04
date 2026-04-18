package com.vsrna.backend.application.round;

import com.vsrna.backend.domain.barrel.Barrel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoundService {
    void startRound(UUID roomId, int roundNumber);
    List<Barrel> getShuffledBarrels(UUID roomId, UUID userId, int roundNumber);
    void purchaseBoost(UUID roomId, UUID userId, int roundNumber);
    void submitSelection(UUID roomId, UUID userId, int roundNumber, List<UUID> barrelIds, Instant timestamp);
    void applyBoostDiscard(UUID roomId, UUID userId, int roundNumber, UUID discardedBarrelId);
    void resolveRound(UUID roomId, int roundNumber);
    void finalizeRound(UUID roomId, int roundNumber);
}
