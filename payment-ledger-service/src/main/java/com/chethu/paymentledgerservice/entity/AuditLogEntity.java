package com.chethu.paymentledgerservice.entity;

import java.time.LocalDateTime;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.domain.AuditTargetType;
import com.chethu.paymentledgerservice.domain.UserRole;
import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs", indexes = { @Index(name = "idx_audit_logs_created_at", columnList = "created_at") })
public class AuditLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actor_user_id", nullable = false) private Long actorUserId;
    @Column(name = "actor_email", nullable = false, length = 255) private String actorEmail;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private UserRole actorRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private AuditAction action;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50) private AuditTargetType targetType;
    @Column(nullable = false) private Long targetId;
    @Column(nullable = false, length = 255) private String reason;
    @Column(columnDefinition = "TEXT") private String metadata;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    protected AuditLogEntity() {}
    public AuditLogEntity(Long actorUserId, String actorEmail, UserRole actorRole, AuditAction action,
            AuditTargetType targetType, Long targetId, String reason, String metadata) {
        this.actorUserId = actorUserId; this.actorEmail = actorEmail; this.actorRole = actorRole; this.action = action;
        this.targetType = targetType; this.targetId = targetId; this.reason = reason; this.metadata = metadata;
    }
    @PrePersist private void timestamp() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; } public Long getActorUserId() { return actorUserId; }
    public String getActorEmail() { return actorEmail; } public UserRole getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; } public AuditTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; } public String getReason() { return reason; }
    public String getMetadata() { return metadata; } public LocalDateTime getCreatedAt() { return createdAt; }
}
