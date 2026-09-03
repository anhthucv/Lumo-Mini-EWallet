package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.chethu.paymentledgerservice.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity,Long>{
    boolean existsByAccountNumber(String accountNumber);
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
    Optional<AccountEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") Long id);
}
