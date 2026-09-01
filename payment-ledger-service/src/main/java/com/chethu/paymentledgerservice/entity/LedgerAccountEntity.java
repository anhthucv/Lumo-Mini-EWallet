package com.chethu.paymentledgerservice.entity;

import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 50)
    private LedgerAccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_class", nullable = false, length = 50)
    private AccountClass accountClass;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_account_id", unique = true)
    private AccountEntity walletAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected LedgerAccountEntity() {
    }

    public LedgerAccountEntity(String code, LedgerAccountType type, AccountClass accountClass,
            AccountEntity walletAccount) {
        if (code == null || code.isBlank() || type == null || accountClass == null) {
            throw new IllegalArgumentException("Ledger account definition is invalid");
        }
        if (type == LedgerAccountType.WALLET && walletAccount == null) {
            throw new IllegalArgumentException("Wallet ledger account must reference a wallet");
        }
        if (type != LedgerAccountType.WALLET && walletAccount != null) {
            throw new IllegalArgumentException("System ledger accounts cannot reference a wallet");
        }
        this.code = code;
        this.type = type;
        this.accountClass = accountClass;
        this.walletAccount = walletAccount;
    }

    @PrePersist
    private void setDefaultTime() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public LedgerAccountType getType() { return type; }
    public AccountClass getAccountClass() { return accountClass; }
    public AccountEntity getWalletAccount() { return walletAccount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
