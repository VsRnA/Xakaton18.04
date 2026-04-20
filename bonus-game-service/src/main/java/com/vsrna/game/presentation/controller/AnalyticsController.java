package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.analytics.AnalyticsService;
import com.vsrna.game.application.analytics.GameAnalyticsSummary;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.presentation.dto.analytics.AnalyticsDto;
import com.vsrna.game.presentation.filter.AuthTokenFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Дашбоард аналитики и экономики игры (ADMIN)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Дашборд аналитики за период",
            description = "KPI сводка и time-series по дням. Параметры `from`/`to` — ISO-8601 UTC. По умолчанию: последние 30 дней."
    )
    @GetMapping
    public ResponseEntity<AnalyticsDto.AnalyticsDashboardResponse> getDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest httpRequest) {

        requireAuth(httpRequest);

        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

        GameAnalyticsSummary summary = analyticsService.getSummary(effectiveFrom, effectiveTo);
        return ResponseEntity.ok(new AnalyticsDto.AnalyticsDashboardResponse(
                AnalyticsDto.AnalyticsSummaryResponse.from(summary),
                analyticsService.getTimeSeries(effectiveFrom, effectiveTo)
        ));
    }

    private UUID requireAuth(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized("bearer token required");
        }
        return userId;
    }
}
