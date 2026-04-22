package com.vsrna.game.presentation.controller;

import com.vsrna.game.application.analytics.AnalyticsService;
import com.vsrna.game.application.analytics.GameAnalyticsSummary;
import com.vsrna.game.application.analytics.GameHistoryEntry;
import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.exception.GameErrorMessages;
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
import java.util.Collection;
import java.util.List;
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

        requireAdminAuth(httpRequest);

        GameAnalyticsSummary summary = analyticsService.getSummary(from, to);
        return ResponseEntity.ok(new AnalyticsDto.AnalyticsDashboardResponse(
                AnalyticsDto.AnalyticsSummaryResponse.from(summary),
                analyticsService.getTimeSeries(from, to)
        ));
    }

    @Operation(summary = "Журнал всех игр (ADMIN)",
            description = "Постраничный список завершённых игр за период. По умолчанию: последние 30 дней.")
    @GetMapping("/games")
    public ResponseEntity<AnalyticsDto.AdminGamesResponse> listGames(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        requireAdminAuth(httpRequest);

        List<GameHistoryEntry> games = analyticsService.listGames(from, to, page, size);
        List<AnalyticsDto.AdminGameRecord> records = games.stream()
                .map(g -> new AnalyticsDto.AdminGameRecord(
                        g.gameRoomId(), g.completedAt(), g.winnerUserId(),
                        g.winnerIsBot(), g.entryFeeAmount(), g.realPlayersRevenue(),
                        g.prizeAwarded(), g.systemBalance(), g.winCriteria(),
                        g.realPlayersCount(), g.botCount(),
                        g.boostAvailable(), g.boostUsedCount(), g.boostRevenue()))
                .toList();
        return ResponseEntity.ok(new AnalyticsDto.AdminGamesResponse(records, records.size()));
    }

    private UUID requireAuth(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized(GameErrorMessages.AUTH_BEARER_REQUIRED);
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    private UUID requireAdminAuth(HttpServletRequest request) {
        UUID userId = requireAuth(request);
        Collection<String> roles = (Collection<String>) request.getAttribute(AuthTokenFilter.ROLES_ATTR);
        if (roles == null || !roles.contains("admin")) {
            throw ApiException.forbidden(GameErrorMessages.AUTH_ADMIN_REQUIRED);
        }
        return userId;
    }
}
