package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
    Optional<IdempotencyRecordEntity> findByAccountAndIdempotencyKey(
            AccountEntity account, String idempotencyKey);
}
