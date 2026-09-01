package com.chethu.paymentledgerservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity,Long>, JpaSpecificationExecutor<TransactionEntity>{
    Optional<TransactionEntity> findByIdAndAccount(Long id, AccountEntity account);
}
    
