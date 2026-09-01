package com.chethu.paymentledgerservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.chethu.paymentledgerservice.domain.LedgerEntryType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "journals")
public class JournalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 100)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.PERSIST, orphanRemoval = false)
    private List<LedgerEntryEntity> entries = new ArrayList<>();

    protected JournalEntity() {
    }

    public JournalEntity(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Journal reference must not be blank");
        }
        this.reference = reference;
    }

    @PrePersist
    private void setDefaultTime() {
        this.createdAt = LocalDateTime.now();
    }

    public void addEntry(LedgerEntryEntity entry) {
        if (entry == null || entry.getJournal() != this) {
            throw new IllegalArgumentException("Ledger entry must belong to this journal");
        }
        entries.add(entry);
    }

    public boolean isBalanced() {
        BigDecimal debits = entries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.DEBIT)
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = entries.stream()
                .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT)
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return !entries.isEmpty() && debits.compareTo(credits) == 0;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<LedgerEntryEntity> getEntries() { return Collections.unmodifiableList(entries); }
}
