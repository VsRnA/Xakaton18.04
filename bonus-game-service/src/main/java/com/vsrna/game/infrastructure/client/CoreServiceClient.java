package com.vsrna.game.infrastructure.client;

import com.vsrna.game.application.port.BalancePort;
import com.vsrna.game.application.port.GameEventPort;
import com.vsrna.game.domain.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
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
            @Value("${app.core.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${app.core.read-timeout-seconds:10}") int readTimeoutSeconds,
            GameEventPort gameEventPort) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        this.restClient = RestClient.builder()
                .baseUrl(coreUrl)
                .requestFactory(factory)
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

    @Override
    public BigDecimal getAvailableBalance(UUID userId) {
        try {
            BalanceResponse response = restClient.get()
                    .uri("/internal/balance/" + userId)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(BalanceResponse.class);
            return response != null && response.available() != null ? response.available() : BigDecimal.ZERO;
        } catch (RestClientResponseException e) {
            log.error("Core service getBalance failed: userId={} status={}", userId, e.getStatusCode());
            throw ApiException.badRequest("Balance check failed: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Core service unavailable: getBalance userId={}", userId, e);
            throw ApiException.internal("Core service unavailable");
        }
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

    public record BalanceResponse(BigDecimal available, BigDecimal reserved) {}
}
