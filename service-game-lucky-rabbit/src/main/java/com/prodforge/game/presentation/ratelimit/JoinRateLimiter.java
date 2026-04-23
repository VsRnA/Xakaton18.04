package com.prodforge.game.presentation.ratelimit;

import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.exception.GameErrorMessages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class JoinRateLimiter {

    @Value("${app.rate-limit.join.max-attempts:50}")
    private int maxAttempts;

    @Value("${app.rate-limit.join.window-seconds:60}")
    private long windowSeconds;

    private final ConcurrentHashMap<UUID, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public void checkAndRecord(UUID userId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);

        Deque<Instant> userAttempts = attempts.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (userAttempts) {
            while (!userAttempts.isEmpty() && userAttempts.peekFirst().isBefore(windowStart)) {
                userAttempts.pollFirst();
            }
            if (userAttempts.size() >= maxAttempts) {
                throw ApiException.tooManyRequests(GameErrorMessages.JOIN_RATE_LIMITED);
            }
            userAttempts.addLast(now);
        }
    }
}
