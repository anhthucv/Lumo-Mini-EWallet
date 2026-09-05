package com.chethu.paymentledgerservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity,Long>, JpaSpecificationExecutor<TransactionEntity>{
    long countByStatus(TransactionStatus status);
    long countByTransactionTypeAndStatus(TransactionType type, TransactionStatus status);

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.transactionType = :type and t.status = :status")
    BigDecimal sumByTypeAndStatus(@Param("type") TransactionType type, @Param("status") TransactionStatus status);

    Optional<TransactionEntity> findByIdAndAccount(Long id, AccountEntity account);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from TransactionEntity t
            where t.account = :account
              and t.transactionType = :type
              and t.status = :status
              and t.createdAt >= :start
              and t.createdAt < :end
            """)
    BigDecimal sumAmountForAccountAndTypeAndStatusBetween(
            @Param("account") AccountEntity account,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            select count(t)
            from TransactionEntity t
            where t.account = :account
              and t.transactionType in :types
              and t.status = :status
              and t.createdAt >= :start
            """)
    long countByAccountAndTypesAndStatusSince(
            @Param("account") AccountEntity account,
            @Param("types") java.util.Collection<TransactionType> types,
            @Param("status") TransactionStatus status,
            @Param("start") LocalDateTime start);
}
    
