package com.chethu.paymentledgerservice.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chethu.paymentledgerservice.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity,Long>{
    @Query ("select t from TransactionEntity t where t.account.id = :accountId")
    Page<TransactionEntity> getHistoryByAccount(Long accountId, Pageable pageable);
}
    

