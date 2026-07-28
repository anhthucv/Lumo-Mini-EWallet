package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;

import com.chethu.paymentledgerservice.domain.AccountStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table (name= "accounts")
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name="account_number",nullable=false, unique=true,length=50)
    private String accountNumber;
    
    @Column(name="owner_name",nullable = false,length=100)
    private String ownerName;

    @Column(name="balance",nullable=false, precision=19, scale=2)
    private BigDecimal  balance;

    @Enumerated(EnumType.STRING) 
    @Column(name="status",nullable = false)
    private AccountStatus status;

    protected AccountEntity(){}

    public AccountEntity(String accountNumber,String ownerName){
        this.ownerName= ownerName;
        this.accountNumber= accountNumber;
        this.status = AccountStatus.ACTIVE;
        this.balance= BigDecimal.ZERO.setScale(2);
    }

    public String getOwnerName(){
        return this.ownerName;
    }

    public String getAccountNumber(){
        return this.accountNumber;
    }

    public BigDecimal getBalance(){
        return this.balance;
    }

    public Long getId(){
        return this.id;
    }

    public AccountStatus getStatus(){
        return this.status;
    }

    public void changeOwnerName(String ownerName){
        this.ownerName= ownerName;
    }

}
