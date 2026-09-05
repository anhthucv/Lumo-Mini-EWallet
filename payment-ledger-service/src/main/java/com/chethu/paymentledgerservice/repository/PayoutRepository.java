package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.PayoutEntity;

import jakarta.persistence.LockModeType;

public interface PayoutRepository extends JpaRepository<PayoutEntity, Long> {
    Optional<PayoutEntity> findByAccountAndIdempotencyKey(AccountEntity account, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayoutEntity p where p.account = :account and p.idempotencyKey = :key")
    Optional<PayoutEntity> findByAccountAndIdempotencyKeyForUpdate(@Param("account") AccountEntity account,
            @Param("key") String key);
}
