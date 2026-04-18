package com.vsrna.backend.application.balance;

import com.vsrna.backend.domain.balance.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

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
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.reserve(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.RESERVE, amount, gameRoomId, "Entry fee reserved"));
    }

    @Override
    @Transactional
    public void deductReserved(UUID userId, BigDecimal amount, UUID gameRoomId) {
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.deduct(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.DEDUCT, amount, gameRoomId, "Entry fee deducted"));
    }

    @Override
    @Transactional
    public void deductPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        // Прямое списание из available (для буста — не из резерва)
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.directDeduct(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.BOOST_PURCHASE, amount, gameRoomId, "Boost purchased"));
    }

    @Override
    @Transactional
    public void creditPoints(UUID userId, BigDecimal amount, UUID gameRoomId) {
        balanceRepository.update(UserBalanceQuery.byUser(userId), UserBalancePatch.credit(amount));
        transactionRepository.create(new PointTransaction(
                userId, TransactionType.AWARD, amount, gameRoomId, "Prize awarded"));
    }
}
