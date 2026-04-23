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

    private static final String IDEMPOTENCY_RESERVE         = "RESERVE";
    private static final String IDEMPOTENCY_RELEASE         = "RELEASE";
    private static final String IDEMPOTENCY_DEDUCT_RESERVED = "DEDUCT_RESERVED";
    private static final String IDEMPOTENCY_BOOST_DEDUCT    = "BOOST_DEDUCT";
    private static final String IDEMPOTENCY_AWARD           = "AWARD";

    private static final String TX_DESC_RESERVE         = "Entry fee reserved";
    private static final String TX_DESC_RELEASE         = "Entry fee returned to available";
    private static final String TX_DESC_DEDUCT_RESERVED = "Entry fee deducted";
    private static final String TX_DESC_BOOST_PURCHASE  = "Boost purchased";
    private static final String TX_DESC_PRIZE_AWARDED   = "Prize awarded";

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
        String key = idempotencyKey(IDEMPOTENCY_RESERVE, userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.reserve(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.RESERVE, amount, gameRoomId, TX_DESC_RESERVE, key));
        log.info("Points reserved: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void returnReservedPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey(IDEMPOTENCY_RELEASE, userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.release(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.DEDUCT, amount, gameRoomId, TX_DESC_RELEASE, key));
        log.info("Points returned: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void deductReserved(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey(IDEMPOTENCY_DEDUCT_RESERVED, userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.deduct(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.DEDUCT, amount, gameRoomId, TX_DESC_DEDUCT_RESERVED, key));
        log.info("Reserved points deducted: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void deductPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey(IDEMPOTENCY_BOOST_DEDUCT, userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.directDeduct(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.BOOST_PURCHASE, amount, gameRoomId, TX_DESC_BOOST_PURCHASE, key));
        log.info("Boost purchased: userId={}, amount={}, roomId={}", userId, amount, gameRoomId);
    }

    @Override
    @Transactional
    public void creditPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        String key = idempotencyKey(IDEMPOTENCY_AWARD, userId, gameRoomId);
        if (isDuplicate(key)) return;
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.credit(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.AWARD, amount, gameRoomId, TX_DESC_PRIZE_AWARDED, key));
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
