package com.chethu.paymentledgerservice.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity,Long>{
    Page<TransactionEntity> findByAccount(AccountEntity account, Pageable pageable);
}
    
