package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.RiskReasonCode;

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
@Table(name = "risk_events")
public class RiskEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private LimitOperationType operationType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private RiskDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 40)
    private RiskReasonCode reasonCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RiskEventEntity() {
    }

    public RiskEventEntity(AccountEntity account, LimitOperationType operationType, BigDecimal amount,
            RiskDecision decision, RiskReasonCode reasonCode) {
        if (account == null || operationType == null || amount == null || decision == null || reasonCode == null) {
            throw new IllegalArgumentException("Risk event definition is invalid");
        }
        this.account = account;
        this.operationType = operationType;
        this.amount = amount;
        this.decision = decision;
        this.reasonCode = reasonCode;
    }

    @PrePersist
    private void setDefaultTime() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AccountEntity getAccount() { return account; }
    public LimitOperationType getOperationType() { return operationType; }
    public BigDecimal getAmount() { return amount; }
    public RiskDecision getDecision() { return decision; }
    public RiskReasonCode getReasonCode() { return reasonCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
