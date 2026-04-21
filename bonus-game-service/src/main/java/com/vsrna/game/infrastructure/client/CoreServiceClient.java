package com.vsrna.game.infrastructure.client;

import com.vsrna.game.application.port.BalancePort;
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

    private static final String ERR_UNAVAILABLE = "Core service unavailable";
    private static final String ERR_BALANCE_CHECK_FAILED = "Balance check failed: ";

    private final RestClient restClient;
    private final String internalSecret;

    public CoreServiceClient(
            @Value("${app.core.url}") String coreUrl,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${app.core.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${app.core.read-timeout-seconds:10}") int readTimeoutSeconds) {
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
            throw ApiException.badRequest(ERR_BALANCE_CHECK_FAILED + e.getStatusCode());
        } catch (Exception e) {
            log.error("Core service unavailable: getBalance userId={}", userId, e);
            throw ApiException.internal(ERR_UNAVAILABLE);
        }
    }

    public record BalanceResponse(BigDecimal available, BigDecimal reserved) {}
}
