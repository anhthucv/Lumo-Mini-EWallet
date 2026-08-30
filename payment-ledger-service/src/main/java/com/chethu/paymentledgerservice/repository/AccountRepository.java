package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity,Long>{
    boolean existsByAccountNumber(String accountNumber);
    Optional<AccountEntity> findByUserId(Long userId);
}
