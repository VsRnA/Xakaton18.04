package com.vsrna.backend.presentation.controller;

import com.vsrna.backend.application.balance.UserBalanceService;
import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.balance.UserBalance;
import com.vsrna.backend.presentation.dto.balance.BalanceDto;
import com.vsrna.backend.presentation.filter.AuthTokenFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/balance")
@RequiredArgsConstructor
@Tag(name = "Balance", description = "Баланс пользователя")
public class BalanceController {

    private final UserBalanceService userBalanceService;

    @Operation(summary = "Получить баланс текущего пользователя")
    @GetMapping
    public BalanceDto.UserBalanceResponse getMyBalance(HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute(AuthTokenFilter.USER_ID_ATTR);
        if (userId == null) {
            throw ApiException.unauthorized("bearer token required");
        }
        UserBalance balance = userBalanceService.getBalance(userId);
        return new BalanceDto.UserBalanceResponse(balance.getAvailable(), balance.getReserved());
    }
}
