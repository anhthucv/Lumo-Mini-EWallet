package com.chethu.paymentledgerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;

public interface AccountRepository extends JpaRepository<AccountEntity,Long>{
    
}
