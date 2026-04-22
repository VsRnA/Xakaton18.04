package com.vsrna.backend.application.balance;

import com.vsrna.backend.domain.balance.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBalanceServiceImpl implements UserBalanceService {

    private final UserBalanceRepository balanceRepository;
    private final PointTransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserBalance getBalance(UUID userId) {
        return balanceRepository.get(UserBalanceQuery.byUser(userId));
    }

    @Override
    @Transactional
    public void reservePoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey("RESERVE", userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.reserve(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.RESERVE, amount, gameRoomId, "Entry fee reserved", key));
        log.info("Points reserved: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void returnReservedPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey("RETURN", userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.release(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.DEDUCT, amount, gameRoomId, "Entry fee returned to available", key));
        log.info("Points returned: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void deductReserved(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey("DEDUCT_RESERVED", userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.deduct(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.DEDUCT, amount, gameRoomId, "Entry fee deducted", key));
        log.info("Reserved points deducted: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void deductPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey("BOOST_DEDUCT", userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.directDeduct(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.BOOST_PURCHASE, amount, gameRoomId, "Boost purchased", key));
        log.info("Boost purchased: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void creditPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey("AWARD", userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.credit(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.AWARD, amount, gameRoomId, "Prize awarded", key));
        log.info("Prize credited: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    private static String idempotencyKey(String operation, UUID userId, UUID gameRoomId) {
        return operation + ":" + userId + ":" + gameRoomId;
    }

    private boolean isDuplicate(String key) {
        if (transactionRepository.findByIdempotencyKey(key).isPresent()) {
            log.info("Idempotent operation skipped, already applied: key={}", key);
            return true;
        }
        return false;
    }
}
