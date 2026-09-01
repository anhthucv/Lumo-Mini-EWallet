package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.domain.TransactionStatus;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntity journal;

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
    this(account, relatedAccount, transactionType, amount, balanceAfterTransaction, TransactionStatus.SUCCESS);
}

    public TransactionEntity(
        AccountEntity account,
        AccountEntity relatedAccount,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfterTransaction,
        TransactionStatus status
) {
    this.account=account;
    this.relatedAccount = relatedAccount;
    this.transactionType = transactionType;
    this.amount = amount;
    this.balanceAfterTransaction = balanceAfterTransaction;
    this.status = status;
}
    protected TransactionEntity(){}

    public Long getId(){return this.id;}
    public AccountEntity getAccount(){return this.account;}
    public AccountEntity getRelatedAccount(){return this.relatedAccount;}
    public TransactionType getTransactionType (){return this.transactionType;}
    public BigDecimal getAmount(){return this.amount;}
    public BigDecimal getBalanceAfterTransaction(){return this.balanceAfterTransaction;}
    public TransactionStatus getStatus(){return this.status;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
    public JournalEntity getJournal(){return this.journal;}

    public void associateJournal(JournalEntity journal) {
        if (journal == null || (this.journal != null && this.journal != journal)) {
            throw new IllegalArgumentException("Transaction journal association is invalid");
        }
        this.journal = journal;
    }

    public void transitionTo(TransactionStatus newStatus) {
        if (!isAllowedTransition(this.status, newStatus)) {
            throw new com.chethu.paymentledgerservice.exception.InvalidTransactionStatusTransitionException(
                    this.status, newStatus);
        }
        this.status = newStatus;
    }

    private boolean isAllowedTransition(TransactionStatus current, TransactionStatus target) {
        if (current == null || target == null) {
            return false;
        }
        return switch (current) {
            case PENDING -> target == TransactionStatus.PROCESSING
                    || target == TransactionStatus.SUCCESS
                    || target == TransactionStatus.FAILED
                    || target == TransactionStatus.CANCELLED;
            case PROCESSING -> target == TransactionStatus.SUCCESS
                    || target == TransactionStatus.FAILED
                    || target == TransactionStatus.CANCELLED;
            case SUCCESS -> target == TransactionStatus.REVERSED;
            case FAILED, CANCELLED, REVERSED -> false;
        };
    }

}
