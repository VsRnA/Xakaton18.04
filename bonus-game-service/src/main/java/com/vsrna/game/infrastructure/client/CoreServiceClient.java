package com.vsrna.game.infrastructure.client;

import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.domain.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class CoreServiceClient implements BalancePort {

    private final RestClient restClient;
    private final String internalSecret;
    private final GameEventPort gameEventPort;

    public CoreServiceClient(
            @Value("${app.core.url}") String coreUrl,
            @Value("${app.internal.secret}") String internalSecret,
            GameEventPort gameEventPort) {
        this.restClient = RestClient.builder()
                .baseUrl(coreUrl)
                .build();
        this.internalSecret = internalSecret;
        this.gameEventPort = gameEventPort;
    }

    @Override
    public void reserve(UUID userId, BigDecimal amount, UUID roomId) {
        call("/internal/balance/reserve", new BalanceRequest(userId, amount, roomId));
        gameEventPort.publishEntryReserved(userId, roomId, amount);
        log.debug("Reserved balance: userId={}, amount={}, roomId={}", userId, amount, roomId);
    }

    @Override
    public void release(UUID userId, BigDecimal amount, UUID roomId) {
        call("/internal/balance/release", new BalanceRequest(userId, amount, roomId));
        log.debug("Released balance: userId={}, amount={}, roomId={}", userId, amount, roomId);
    }

    @Override
    public void award(UUID userId, BigDecimal amount, UUID roomId) {
        call("/internal/balance/award", new BalanceRequest(userId, amount, roomId));
        log.debug("Awarded balance: userId={}, amount={}, roomId={}", userId, amount, roomId);
    }

    @Override
    public void deduct(UUID userId, BigDecimal amount, UUID roomId) {
        call("/internal/balance/deduct", new BalanceRequest(userId, amount, roomId));
        log.debug("Deducted balance: userId={}, amount={}, roomId={}", userId, amount, roomId);
    }

    private void call(String path, Object body) {
        try {
            restClient.post()
                    .uri(path)
                    .header("X-Internal-Secret", internalSecret)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("Core service call failed: {} → status={}, body={}",
                    path, e.getStatusCode(), e.getResponseBodyAsString());
            throw ApiException.badRequest("Balance operation failed: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Core service unavailable: {}", path, e);
            throw ApiException.internal("Core service unavailable");
        }
    }

    public record BalanceRequest(UUID userId, BigDecimal amount, UUID roomId) {}
}
