package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.entity.AuditLogEntity;
import com.chethu.paymentledgerservice.exception.AdminAuditLogNotFoundException;
import com.chethu.paymentledgerservice.repository.AuditLogRepository;

class AdminAuditLogServiceTest {
    @Test
    void list_shouldUseDatabaseSpecificationAndMapSafeFields() {
        AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
        AuditLogEntity log = new AuditLogEntity(1L, "admin@example.com", com.chethu.paymentledgerservice.domain.UserRole.ADMIN,
                AuditAction.ADMIN_USER_LOCK, com.chethu.paymentledgerservice.domain.AuditTargetType.USER, 9L, "policy", "ACTIVE -> LOCKED");
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        var result = new AdminAuditLogService(repository).list("admin", AuditAction.ADMIN_USER_LOCK, null, null, PageRequest.of(0, 10));

        assertEquals("admin@example.com", result.getContent().get(0).actorEmail());
        assertEquals("ACTIVE -> LOCKED", result.getContent().get(0).metadata());
    }

    @Test
    void detail_shouldRejectUnknownAuditLog() {
        AuditLogRepository repository = Mockito.mock(AuditLogRepository.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(AdminAuditLogNotFoundException.class, () -> new AdminAuditLogService(repository).detail(99L));
    }
}
