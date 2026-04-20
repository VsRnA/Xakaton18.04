package com.vsrna.backend.infrastructure.persistence.balance;

import com.vsrna.backend.domain.balance.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface UserBalanceJpaRepository extends JpaRepository<UserBalance, UUID> {

    // Атомарное резервирование: available → reserved, защита от overdraft + @Version
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.available = b.available - :delta,
                b.reserved  = b.reserved  + :delta,
                b.version   = b.version   + 1,
                b.updatedAt = :now
            WHERE b.userId = :userId
              AND b.available >= :delta
              AND b.version = :version
            """)
    int reserve(@Param("userId") UUID userId,
                @Param("delta") BigDecimal delta,
                @Param("version") long version,
                @Param("now") Instant now);

    // Освобождение резерва: reserved → available
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.reserved  = b.reserved  - :delta,
                b.available = b.available + :delta,
                b.version   = b.version   + 1,
                b.updatedAt = :now
            WHERE b.userId = :userId
              AND b.reserved >= :delta
              AND b.version = :version
            """)
    int release(@Param("userId") UUID userId,
                @Param("delta") BigDecimal delta,
                @Param("version") long version,
                @Param("now") Instant now);

    // Финальное списание из reserved
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.reserved  = b.reserved - :delta,
                b.version   = b.version  + 1,
                b.updatedAt = :now
            WHERE b.userId = :userId
              AND b.reserved >= :delta
              AND b.version = :version
            """)
    int deductReserved(@Param("userId") UUID userId,
                       @Param("delta") BigDecimal delta,
                       @Param("version") long version,
                       @Param("now") Instant now);

    // Прямое списание из available (для буста)
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.available = b.available - :delta,
                b.version   = b.version   + 1,
                b.updatedAt = :now
            WHERE b.userId = :userId
              AND b.available >= :delta
              AND b.version = :version
            """)
    int deduct(@Param("userId") UUID userId,
               @Param("delta") BigDecimal delta,
               @Param("version") long version,
               @Param("now") Instant now);

    // Зачисление в available (выигрыш)
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.available = b.available + :delta,
                b.version   = b.version   + 1,
                b.updatedAt = :now
            WHERE b.userId = :userId
              AND b.version = :version
            """)
    int credit(@Param("userId") UUID userId,
               @Param("delta") BigDecimal delta,
               @Param("version") long version,
               @Param("now") Instant now);
}
