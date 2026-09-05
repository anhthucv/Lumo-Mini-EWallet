package com.chethu.paymentledgerservice.dto;
import java.time.LocalDateTime;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.domain.AuditTargetType;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.entity.AuditLogEntity;
public record AdminAuditLogResponse(Long auditId, LocalDateTime createdAt, Long actorUserId, String actorEmail,
        UserRole actorRole, AuditAction action, AuditTargetType targetType, Long targetId, String reason, String metadata) {
    public static AdminAuditLogResponse from(AuditLogEntity a) { return new AdminAuditLogResponse(a.getId(), a.getCreatedAt(), a.getActorUserId(),
            a.getActorEmail(), a.getActorRole(), a.getAction(), a.getTargetType(), a.getTargetId(), a.getReason(), a.getMetadata()); }
}
