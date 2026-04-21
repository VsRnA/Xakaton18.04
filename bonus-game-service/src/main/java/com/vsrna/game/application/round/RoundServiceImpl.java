package com.vsrna.game.application.round;

import com.vsrna.game.domain.barrel.Barrel;
import com.vsrna.game.domain.history.GameHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    private final RoundLifecycleService lifecycleService;
    private final SelectionService selectionService;
    private final BoostService boostService;
    private final RoundResultService roundResultService;

    @Override
    public void startRound(UUID roomId, int roundNumber) {
        lifecycleService.startRound(roomId, roundNumber);
    }

    @Override
    public List<Barrel> getShuffledBarrels(UUID roomId, UUID userId, int roundNumber) {
        return selectionService.getShuffledBarrels(roomId, userId, roundNumber);
    }

    @Override
    public void purchaseBoost(UUID roomId, UUID userId, int roundNumber) {
        boostService.purchaseBoost(roomId, userId, roundNumber);
    }

    @Override
    public void submitSelection(UUID roomId, UUID userId, int roundNumber,
                                List<UUID> barrelIds, Instant timestamp) {
        selectionService.submitSelection(roomId, userId, roundNumber, barrelIds, timestamp);
    }

    @Override
    public void resolveRound(UUID roomId, int roundNumber) {
        lifecycleService.resolveRound(roomId, roundNumber);
    }

    @Override
    public void startBoostWindow(UUID roomId, int roundNumber) {
        lifecycleService.startBoostWindow(roomId, roundNumber);
    }

    @Override
    public void finalizeRound(UUID roomId, int roundNumber) {
        lifecycleService.finalizeRound(roomId, roundNumber);
    }

    @Override
    public void markFinalistReady(UUID roomId, UUID userId) {
        lifecycleService.markFinalistReady(roomId, userId);
    }

    @Override
    public void startRound2AfterTimeout(UUID roomId) {
        lifecycleService.startRound2AfterTimeout(roomId);
    }

    @Override
    public RoundResultDetails getRoundResult(UUID roomId, int roundNumber) {
        return roundResultService.getRoundResult(roomId, roundNumber);
    }

    @Override
    public GameHistory getGameHistory(UUID roomId) {
        return roundResultService.getGameHistory(roomId);
    }

    @Override
    public GameHistoryDetails getGameHistoryDetails(UUID roomId) {
        return roundResultService.getGameHistoryDetails(roomId);
    }
}
