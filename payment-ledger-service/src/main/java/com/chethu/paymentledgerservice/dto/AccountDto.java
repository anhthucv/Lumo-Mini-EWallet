package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import com.chethu.paymentledgerservice.domain.AccountStatus;

public class AccountDto {
    private Long id;
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;
    private AccountStatus status;

    public AccountDto(Long id, String accountNumber, String ownerName, BigDecimal balance, AccountStatus status){
        this.id = id;
        this.accountNumber= accountNumber;
        this.ownerName= ownerName;
        this.balance = balance;
        this.status = status;
        
    }

    public Long getId(){
        return this.id;
    }

    public String getAccountNumber(){
        return this.accountNumber;
    }

    public String getOwnerName(){
        return this.ownerName;
    }

    public BigDecimal getBalance(){
        return this.balance;
    }

    public AccountStatus getStatus(){
        return this.status;
    }
}
