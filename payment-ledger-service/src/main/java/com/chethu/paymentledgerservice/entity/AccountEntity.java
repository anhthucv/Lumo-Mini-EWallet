package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.exception.InsufficientBalanceException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;


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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserEntity user;

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

    public UserEntity getUser() {
        return user;
    }

    public void getUser(UserEntity user) {
        this.user = user;
    }

    public void changeOwnerName(String ownerName){
        this.ownerName= ownerName;
    }

    public void deposit(BigDecimal amount){
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount, BigDecimal minimumBalance){
        BigDecimal newBalance = this.balance.subtract(amount);
        if (newBalance.compareTo(minimumBalance)<0) 
            throw new InsufficientBalanceException();
        this.balance = newBalance;
    }

}
