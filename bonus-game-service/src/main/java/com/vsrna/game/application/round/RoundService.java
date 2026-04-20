package com.vsrna.game.application.round;

import com.vsrna.game.domain.barrel.Barrel;
import com.vsrna.game.domain.history.GameHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoundService {
    void startRound(UUID roomId, int roundNumber);
    List<Barrel> getShuffledBarrels(UUID roomId, UUID userId, int roundNumber);
    void purchaseBoost(UUID roomId, UUID userId, int roundNumber);
    void submitSelection(UUID roomId, UUID userId, int roundNumber, List<UUID> barrelIds, Instant timestamp);
    void applyBoost(UUID roomId, UUID userId, int roundNumber, UUID boostedBarrelId);
    void resolveRound(UUID roomId, int roundNumber);
    void startBoostWindow(UUID roomId, int roundNumber);
    void finalizeRound(UUID roomId, int roundNumber);
    RoundResultDetails getRoundResult(UUID roomId, int roundNumber);
    GameHistory getGameHistory(UUID roomId);
}
