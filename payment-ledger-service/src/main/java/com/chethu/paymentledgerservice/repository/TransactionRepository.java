package com.chethu.paymentledgerservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.chethu.paymentledgerservice.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity,Long>, JpaSpecificationExecutor<TransactionEntity>{
}
    
