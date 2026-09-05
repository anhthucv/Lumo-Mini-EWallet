package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;

public interface TopUpPaymentRepository extends JpaRepository<TopUpPaymentEntity, Long> {
    Optional<TopUpPaymentEntity> findByAccountAndIdempotencyKey(AccountEntity account, String idempotencyKey);
    Optional<TopUpPaymentEntity> findByIdAndAccount(Long id, AccountEntity account);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TopUpPaymentEntity p where p.id = :id and p.account = :account")
    Optional<TopUpPaymentEntity> findByIdAndAccountForUpdate(@Param("id") Long id,
            @Param("account") AccountEntity account);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TopUpPaymentEntity p where p.merchantOrderCode = :merchantOrderCode")
    Optional<TopUpPaymentEntity> findByMerchantOrderCodeForUpdate(@Param("merchantOrderCode") Long merchantOrderCode);
    Optional<TopUpPaymentEntity> findByProviderReference(String providerReference);
    Optional<TopUpPaymentEntity> findByTransactionId(Long transactionId);
}
