package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.PayoutProviderType;
import com.chethu.paymentledgerservice.domain.PayoutStatus;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "payouts", uniqueConstraints = @UniqueConstraint(
        name = "uk_payout_account_idempotency", columnNames = { "account_id", "idempotency_key" }))
public class PayoutEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutProviderType provider = PayoutProviderType.PAYOS;

    @Column(nullable = false, unique = true, length = 100)
    private String merchantReference;

    @Column(length = 255)
    private String providerReference;

    @Column(nullable = false, length = 20)
    private String destinationBankIdentifier;

    @Column(nullable = false, length = 20)
    private String destinationAccountSummary;

    @Column(nullable = false, length = 64)
    private String destinationAccountHash;

    @Column(nullable = false, length = 500)
    private String destinationAccountEncrypted;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_journal_id", nullable = false, unique = true)
    private JournalEntity reservationJournal;

    @Column(nullable = false)
    private boolean providerRequestStarted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected PayoutEntity() {
    }

    public PayoutEntity(AccountEntity account, BigDecimal amount, String merchantReference,
            String destinationBankIdentifier, String destinationAccountSummary, String destinationAccountHash,
            String destinationAccountEncrypted, String idempotencyKey, JournalEntity reservationJournal) {
        this.account = account;
        this.amount = amount;
        this.merchantReference = merchantReference;
        this.destinationBankIdentifier = destinationBankIdentifier;
        this.destinationAccountSummary = destinationAccountSummary;
        this.destinationAccountHash = destinationAccountHash;
        this.destinationAccountEncrypted = destinationAccountEncrypted;
        this.idempotencyKey = idempotencyKey;
        this.reservationJournal = reservationJournal;
    }

    @PrePersist
    private void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    private void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void markProviderRequestStarted() { providerRequestStarted = true; }
    public void attachProviderReference(String providerReference) { this.providerReference = providerReference; }
    public Long getId() { return id; }
    public AccountEntity getAccount() { return account; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PayoutStatus getStatus() { return status; }
    public PayoutProviderType getProvider() { return provider; }
    public String getMerchantReference() { return merchantReference; }
    public String getProviderReference() { return providerReference; }
    public String getDestinationBankIdentifier() { return destinationBankIdentifier; }
    public String getDestinationAccountSummary() { return destinationAccountSummary; }
    public String getDestinationAccountHash() { return destinationAccountHash; }
    public String getDestinationAccountEncrypted() { return destinationAccountEncrypted; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public JournalEntity getReservationJournal() { return reservationJournal; }
    public boolean isProviderRequestStarted() { return providerRequestStarted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
