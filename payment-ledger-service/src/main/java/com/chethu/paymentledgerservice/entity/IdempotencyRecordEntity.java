package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "idempotency_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_account_key",
        columnNames = { "account_id", "idempotency_key" }))
public class IdempotencyRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private IdempotencyOperationType operationType;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestAmount;

    @Column(name = "recipient_account_number", length = 50)
    private String recipientAccountNumber;

    @Column(name = "result_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal resultBalance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntity journal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected IdempotencyRecordEntity() {
    }

    public IdempotencyRecordEntity(AccountEntity account, IdempotencyOperationType operationType,
            String idempotencyKey, BigDecimal requestAmount, String recipientAccountNumber,
            BigDecimal resultBalance, JournalEntity journal) {
        this.account = account;
        this.operationType = operationType;
        this.idempotencyKey = idempotencyKey;
        this.requestAmount = requestAmount;
        this.recipientAccountNumber = recipientAccountNumber;
        this.resultBalance = resultBalance;
        this.journal = journal;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AccountEntity getAccount() { return account; }
    public IdempotencyOperationType getOperationType() { return operationType; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getRequestAmount() { return requestAmount; }
    public String getRecipientAccountNumber() { return recipientAccountNumber; }
    public BigDecimal getResultBalance() { return resultBalance; }
    public JournalEntity getJournal() { return journal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
