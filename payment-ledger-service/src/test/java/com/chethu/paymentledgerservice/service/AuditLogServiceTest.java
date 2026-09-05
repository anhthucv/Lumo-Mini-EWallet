package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.entity.AuditLogEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.repository.AuditLogRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

class AuditLogServiceTest {
    @Test
    void recordUserStatusChange_shouldDeriveActorSnapshotAndTargetFromArguments() {
        AuditLogRepository logs = Mockito.mock(AuditLogRepository.class);
        UserRepository users = Mockito.mock(UserRepository.class);
        UserEntity actor = new UserEntity("admin@example.com", "hash", "Admin", UserRole.ADMIN, UserStatus.ACTIVE);
        try { Field id = UserEntity.class.getDeclaredField("id"); id.setAccessible(true); id.set(actor, 1L); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        when(users.findById(1L)).thenReturn(Optional.of(actor));
        when(logs.save(any(AuditLogEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLogEntity result = new AuditLogService(logs, users).recordUserStatusChange(1L, 9L,
                AuditAction.ADMIN_USER_LOCK, "policy", "ACTIVE -> LOCKED");

        assertEquals(1L, result.getActorUserId());
        assertEquals("admin@example.com", result.getActorEmail());
        assertEquals(9L, result.getTargetId());
        assertEquals("policy", result.getReason());
        verify(logs).save(any(AuditLogEntity.class));
    }
}
