package com.chethu.paymentledgerservice.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.dto.AdminAuditLogResponse;
import com.chethu.paymentledgerservice.entity.AuditLogEntity;
import com.chethu.paymentledgerservice.exception.AdminAuditLogNotFoundException;
import com.chethu.paymentledgerservice.repository.AuditLogRepository;
@Service
public class AdminAuditLogService {
    private final AuditLogRepository repository;
    public AdminAuditLogService(AuditLogRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> list(String search, AuditAction action, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("from must be on or before to");
        Specification<AuditLogEntity> spec = (r,q,cb) -> cb.conjunction();
        if (action != null) spec = spec.and((r,q,cb) -> cb.equal(r.get("action"), action));
        if (from != null) { LocalDateTime value = from.atStartOfDay(); spec = spec.and((r,q,cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), value)); }
        if (to != null) { LocalDateTime value = to.plusDays(1).atStartOfDay(); spec = spec.and((r,q,cb) -> cb.lessThan(r.get("createdAt"), value)); }
        if (search != null && !search.isBlank()) { String value = "%" + search.trim().toLowerCase() + "%"; spec = spec.and((r,q,cb) -> cb.or(cb.like(cb.lower(r.get("actorEmail")), value), cb.like(cb.lower(r.get("reason")), value))); }
        return repository.findAll(spec, pageable).map(AdminAuditLogResponse::from);
    }
    @Transactional(readOnly = true)
    public AdminAuditLogResponse detail(Long id) { return repository.findById(id).map(AdminAuditLogResponse::from)
            .orElseThrow(() -> new AdminAuditLogNotFoundException(id)); }
}
