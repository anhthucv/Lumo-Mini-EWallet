package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.LedgerEntryType;

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
import org.hibernate.annotations.Check;

@Entity
@Table(name = "ledger_entries")
@Check(name = "ck_ledger_entries_amount_positive", constraints = "amount > 0")
public class LedgerEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntity journal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccountEntity ledgerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected LedgerEntryEntity() {
    }

    public LedgerEntryEntity(JournalEntity journal, LedgerAccountEntity ledgerAccount,
            LedgerEntryType entryType, BigDecimal amount) {
        if (journal == null || ledgerAccount == null || entryType == null
                || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Ledger entry definition is invalid");
        }
        this.journal = journal;
        this.ledgerAccount = ledgerAccount;
        this.entryType = entryType;
        this.amount = amount;
        journal.addEntry(this);
    }

    @PrePersist
    private void setDefaultTime() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public JournalEntity getJournal() { return journal; }
    public LedgerAccountEntity getLedgerAccount() { return ledgerAccount; }
    public LedgerEntryType getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
