package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, Long> {
    Optional<LedgerAccountEntity> findByCode(String code);
    Optional<LedgerAccountEntity> findByWalletAccount(AccountEntity walletAccount);
}
