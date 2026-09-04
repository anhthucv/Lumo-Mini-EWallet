package com.chethu.paymentledgerservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "beneficiaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_beneficiaries_owner_account", columnNames = { "owner_id", "beneficiary_account_id" })
})
public class BeneficiaryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_account_id", nullable = false)
    private AccountEntity beneficiaryAccount;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BeneficiaryEntity() {
    }

    public BeneficiaryEntity(UserEntity owner, AccountEntity beneficiaryAccount, String nickname) {
        if (owner == null || beneficiaryAccount == null || nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Beneficiary definition is invalid");
        }
        this.owner = owner;
        this.beneficiaryAccount = beneficiaryAccount;
        this.nickname = nickname;
    }

    @PrePersist
    private void setCreatedAndUpdatedAt() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void setUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UserEntity getOwner() { return owner; }
    public AccountEntity getBeneficiaryAccount() { return beneficiaryAccount; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void changeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname must not be blank");
        }
        this.nickname = nickname;
    }
}
