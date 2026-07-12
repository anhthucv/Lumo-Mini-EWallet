package com.chethu.paymentledgerservice.domain;

import java.math.BigDecimal;

public class Account {
    private Long id;
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;
    private AccountStatus status;

    public Account(Long id, String accountNumber, String ownerName){
        if (ownerName == null || ownerName.isBlank()){
            throw new IllegalArgumentException("Owner name must not be blank");
        }
        this.id = id;
        this.accountNumber= accountNumber;
        this.ownerName= ownerName;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = AccountStatus.ACTIVE;
        
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
