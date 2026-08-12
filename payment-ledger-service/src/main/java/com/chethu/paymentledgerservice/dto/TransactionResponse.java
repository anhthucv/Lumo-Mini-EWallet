package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.TransactionType;

public class TransactionResponse {
    private Long id;
    private Long accountId;
    private Long relatedAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfterTransaction;
    private LocalDateTime createdAt;

    public TransactionResponse(Long id, Long accountId, Long relatedAccountId, TransactionType transactionType, BigDecimal amount,BigDecimal balanceAfterTransaction, LocalDateTime createdAt){
        this.id = id;
        this.accountId = accountId;
        this.relatedAccountId = relatedAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.createdAt = createdAt;
    }   
    public Long getId() {return this.id;}
    public Long getAccountId() {return this.accountId;}
    public Long getRelatedAccountId (){return this.relatedAccountId;}
    public TransactionType getTransactionType(){return this.transactionType;}
    public BigDecimal getAmount(){return this.amount;}
    public BigDecimal getBalanceAfterTransaction(){return this.balanceAfterTransaction;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
}
