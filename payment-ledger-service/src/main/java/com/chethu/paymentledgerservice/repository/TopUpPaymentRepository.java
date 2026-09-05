package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;

public interface TopUpPaymentRepository extends JpaRepository<TopUpPaymentEntity, Long> {
    Optional<TopUpPaymentEntity> findByAccountAndIdempotencyKey(AccountEntity account, String idempotencyKey);
    Optional<TopUpPaymentEntity> findByMerchantOrderCode(Long merchantOrderCode);
    Optional<TopUpPaymentEntity> findByProviderReference(String providerReference);
}
