package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "top_up_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_top_up_account_idempotency", columnNames = { "account_id", "idempotency_key" })
})
public class TopUpPaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TopUpPaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentProviderType provider;

    @Column(name = "merchant_order_code", unique = true)
    private Long merchantOrderCode;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TopUpPaymentEntity() {
    }

    public TopUpPaymentEntity(AccountEntity account, BigDecimal amount, String idempotencyKey) {
        this.account = account;
        this.amount = amount;
        this.currency = "VND";
        this.status = TopUpPaymentStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void assignMerchantOrderCode() {
        if (id == null || id <= 0) {
            throw new IllegalStateException("Top-up payment identifier is required");
        }
        merchantOrderCode = id;
    }

    public void attachCheckout(PaymentCheckoutResult checkout) {
        if (checkout == null || checkout.merchantOrderCode() != merchantOrderCode) {
            throw new IllegalArgumentException("Payment checkout order code does not match top-up payment");
        }
        provider = checkout.provider();
        providerReference = checkout.providerReference();
        checkoutUrl = checkout.checkoutUrl();
    }

    public Long getId() { return id; }
    public AccountEntity getAccount() { return account; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TopUpPaymentStatus getStatus() { return status; }
    public PaymentProviderType getProvider() { return provider; }
    public Long getMerchantOrderCode() { return merchantOrderCode; }
    public String getProviderReference() { return providerReference; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
