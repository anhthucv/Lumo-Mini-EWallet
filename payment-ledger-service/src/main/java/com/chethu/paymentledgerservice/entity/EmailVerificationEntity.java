package com.chethu.paymentledgerservice.entity;

import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.EmailVerificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_codes", indexes = {
        @Index(name = "idx_email_verification_email_created_at", columnList = "email, created_at"),
        @Index(name = "idx_email_verification_email_status_created_at", columnList = "email, status, created_at")
})
public class EmailVerificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EmailVerificationStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EmailVerificationEntity() {
    }

    public EmailVerificationEntity(String email, String codeHash, EmailVerificationStatus status, int failedAttempts,
            LocalDateTime expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    private void setDefaultTime() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public EmailVerificationStatus getStatus() {
        return status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void invalidate() {
        this.status = EmailVerificationStatus.INVALIDATED;
    }

    public void markUsed() {
        this.status = EmailVerificationStatus.USED;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
}
