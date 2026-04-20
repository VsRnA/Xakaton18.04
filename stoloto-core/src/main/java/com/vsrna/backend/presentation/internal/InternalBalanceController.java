package com.vsrna.backend.presentation.internal;

import com.vsrna.backend.application.balance.UserBalanceService;
import com.vsrna.backend.application.user.UserService;
import com.vsrna.backend.domain.balance.UserBalance;
import com.vsrna.backend.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalBalanceController {

    private final UserBalanceService userBalanceService;
    private final UserService userService;

    @PostMapping("/balance/reserve")
    public ResponseEntity<Map<String, Object>> reserve(@RequestBody BalanceOperationRequest request) {
        userBalanceService.reservePoints(request.userId(), request.amount(), request.roomId());
        UserBalance balance = userBalanceService.getBalance(request.userId());
        return ResponseEntity.ok(Map.of(
                "userId", request.userId(),
                "available", balance.getAvailable(),
                "reserved", balance.getReserved()
        ));
    }

    @PostMapping("/balance/release")
    public ResponseEntity<Map<String, Object>> release(@RequestBody BalanceOperationRequest request) {
        userBalanceService.returnReservedPoints(request.userId(), request.amount(), request.roomId());
        UserBalance balance = userBalanceService.getBalance(request.userId());
        return ResponseEntity.ok(Map.of(
                "userId", request.userId(),
                "available", balance.getAvailable(),
                "reserved", balance.getReserved()
        ));
    }

    @PostMapping("/balance/award")
    public ResponseEntity<Map<String, Object>> award(@RequestBody BalanceOperationRequest request) {
        userBalanceService.creditPoints(request.userId(), request.amount(), request.roomId());
        UserBalance balance = userBalanceService.getBalance(request.userId());
        return ResponseEntity.ok(Map.of(
                "userId", request.userId(),
                "available", balance.getAvailable(),
                "reserved", balance.getReserved()
        ));
    }

    @PostMapping("/balance/deduct")
    public ResponseEntity<Map<String, Object>> deduct(@RequestBody BalanceOperationRequest request) {
        userBalanceService.deductPoints(request.userId(), request.amount(), request.roomId());
        UserBalance balance = userBalanceService.getBalance(request.userId());
        return ResponseEntity.ok(Map.of(
                "userId", request.userId(),
                "available", balance.getAvailable(),
                "reserved", balance.getReserved()
        ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable UUID userId) {
        User user = userService.getUser(userId);
        return ResponseEntity.ok(Map.of(
                "id", user.getGuid(),
                "username", user.getUsername()
        ));
    }

    public record BalanceOperationRequest(UUID userId, BigDecimal amount, UUID roomId) {}
}
