package com.prodforge.game.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GameMetrics {

    private final MeterRegistry registry;

    public final Counter roomsCreatedImmediate;
    public final Counter roomsCreatedScheduled;
    public final Counter roomsStarted;
    public final Counter roomsCancelled;

    public final Counter playersJoined;
    public final Counter playersDisqualified;

    public final Counter round1Started;
    public final Counter round2Started;

    public final Counter boostsPurchased;

    public final Counter prizeAwardedPoints;
    public final Counter systemRevenuePoints;

    public GameMetrics(MeterRegistry registry) {
        this.registry = registry;

        roomsCreatedImmediate = Counter.builder("game_rooms_created_total")
                .tag("scheduled", "false")
                .description("Total game rooms created")
                .register(registry);
        roomsCreatedScheduled = Counter.builder("game_rooms_created_total")
                .tag("scheduled", "true")
                .description("Total game rooms created")
                .register(registry);

        roomsStarted = Counter.builder("game_rooms_started_total")
                .description("Total game rooms started (bots filled, round 1 began)")
                .register(registry);

        roomsCancelled = Counter.builder("game_rooms_cancelled_total")
                .description("Total game rooms cancelled by admin")
                .register(registry);

        playersJoined = Counter.builder("game_players_joined_total")
                .description("Total real player join events")
                .register(registry);

        playersDisqualified = Counter.builder("game_players_disqualified_total")
                .description("Total players disqualified for missing barrel selection")
                .register(registry);

        round1Started = Counter.builder("game_rounds_started_total")
                .tag("round", "1")
                .description("Total rounds started")
                .register(registry);

        round2Started = Counter.builder("game_rounds_started_total")
                .tag("round", "2")
                .description("Total rounds started")
                .register(registry);

        boostsPurchased = Counter.builder("game_boosts_purchased_total")
                .description("Total boost purchases")
                .register(registry);

        prizeAwardedPoints = Counter.builder("game_prize_awarded_points_total")
                .description("Cumulative bonus points awarded to real-player winners")
                .register(registry);

        systemRevenuePoints = Counter.builder("game_system_revenue_points_total")
                .description("Cumulative bonus points retained as system revenue")
                .register(registry);
    }

    public void recordRoomFinished(boolean winnerIsBot) {
        registry.counter("game_rooms_finished_total",
                "winner_type", winnerIsBot ? "bot" : "real").increment();
    }

    public void recordRoundCompleted(int roundNumber, String winCriteria) {
        registry.counter("game_rounds_completed_total",
                "round", String.valueOf(roundNumber),
                "win_criteria", winCriteria).increment();
    }
}
