package com.chethu.paymentledgerservice.service;
import org.springframework.stereotype.Service;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.domain.AuditTargetType;
import com.chethu.paymentledgerservice.entity.AuditLogEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.repository.AuditLogRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;
@Service
public class AuditLogService {
    private final AuditLogRepository repository; private final UserRepository users;
    public AuditLogService(AuditLogRepository repository, UserRepository users) { this.repository = repository; this.users = users; }
    public AuditLogEntity recordUserStatusChange(Long actorId, Long targetId, AuditAction action, String reason, String metadata) {
        UserEntity actor = users.findById(actorId).orElseThrow();
        return repository.save(new AuditLogEntity(actor.getId(), actor.getEmail(), actor.getRole(), action,
                AuditTargetType.USER, targetId, reason, metadata));
    }
}
