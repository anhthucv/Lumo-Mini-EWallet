package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.chethu.paymentledgerservice.domain.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table (name="transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "account_id",nullable = false)
    private AccountEntity account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_account_id")
    private AccountEntity relatedAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length=50)
    private TransactionType transactionType;

    @Column(name = "amount",nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after_transaction",nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfterTransaction;

    @Column(name ="created_at", nullable =  false, updatable = false )
    private LocalDateTime createdAt;

    @PrePersist
    private void setDefaultTime() {
        this.createdAt = LocalDateTime.now();
    }

    public TransactionEntity(
        AccountEntity account,
        AccountEntity relatedAccount,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfterTransaction
) {
    this.account=account;
    this.relatedAccount = relatedAccount;
    this.transactionType = transactionType;
    this.amount = amount;
    this.balanceAfterTransaction = balanceAfterTransaction;
}
    protected TransactionEntity(){}

    public Long getId(){return this.id;}
    public AccountEntity getAccount(){return this.account;}
    public AccountEntity getRelatedAccount(){return this.relatedAccount;}
    public TransactionType getTransactionType (){return this.transactionType;}
    public BigDecimal getAmount(){return this.amount;}
    public BigDecimal getBalanceAfterTransaction(){return this.balanceAfterTransaction;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

}
