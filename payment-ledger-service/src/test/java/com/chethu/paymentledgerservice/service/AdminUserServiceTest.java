package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.dto.AdminUserResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.AdminUserOperationForbiddenException;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

class AdminUserServiceTest {
    @Test
    void listUsers_shouldUseDatabasePageAndReturnOnlySafeMaskedFields() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        AuditLogService audit = Mockito.mock(AuditLogService.class);
        AdminUserService service = new AdminUserService(users, accounts, audit);
        UserEntity user = user(7L, UserRole.USER, UserStatus.ACTIVE);
        AccountEntity account = new AccountEntity("1234567890", "Owner");
        setField(account, "id", 8L);
        account.deposit(new BigDecimal("1250.00"));
        when(users.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        when(accounts.findByUserId(7L)).thenReturn(Optional.of(account));

        AdminUserResponse response = service.listUsers(null, PageRequest.of(0, 500)).getContent().get(0);

        assertEquals("****7890", response.accountNumberSummary());
        assertEquals(new BigDecimal("1250.00"), response.balance());
        assertEquals(10, AdminUserResponse.class.getRecordComponents().length);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(users).findAll(pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void listUsers_shouldDelegateCaseInsensitiveSearchToRepository() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        AdminUserService service = new AdminUserService(users, accounts, Mockito.mock(AuditLogService.class));
        when(users.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.listUsers("  Alice  ", PageRequest.of(1, 10));

        verify(users).findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase("Alice", "Alice",
                PageRequest.of(1, 10, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        verify(users, never()).findAll(any(Pageable.class));
    }

    @Test
    void lockUser_shouldPersistLockedStatusAndTrimReason() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        AuditLogService audit = Mockito.mock(AuditLogService.class);
        AdminUserService service = new AdminUserService(users, accounts, audit);
        UserEntity target = user(7L, UserRole.USER, UserStatus.ACTIVE);
        when(users.findById(7L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);
        when(accounts.findByUserId(7L)).thenReturn(Optional.empty());

        AdminUserResponse response = service.lockUser(1L, 7L, "  policy violation  ");

        assertEquals(UserStatus.LOCKED, target.getStatus());
        assertEquals("policy violation", target.getStatusReason());
        assertEquals(UserStatus.LOCKED, response.status());
        verify(users).save(target);
        verify(audit, times(1)).recordUserStatusChange(1L, 7L, AuditAction.ADMIN_USER_LOCK,
                "policy violation", "ACTIVE -> LOCKED");
    }

    @Test
    void unlockUser_shouldPersistActiveStatusAndReason() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        AdminUserService service = new AdminUserService(users, accounts, Mockito.mock(AuditLogService.class));
        UserEntity target = user(7L, UserRole.USER, UserStatus.LOCKED);
        when(users.findById(7L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);
        when(accounts.findByUserId(7L)).thenReturn(Optional.empty());

        service.unlockUser(1L, 7L, "manual review");

        assertEquals(UserStatus.ACTIVE, target.getStatus());
        assertEquals("manual review", target.getStatusReason());
    }

    @Test
    void statusChange_shouldRejectSelf() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AdminUserService service = new AdminUserService(users, Mockito.mock(AccountRepository.class), Mockito.mock(AuditLogService.class));
        UserEntity target = user(7L, UserRole.ADMIN, UserStatus.ACTIVE);
        when(users.findById(7L)).thenReturn(Optional.of(target));

        assertThrows(AdminUserOperationForbiddenException.class, () -> service.lockUser(7L, 7L, "reason"));
        verify(users, never()).save(any());
    }

    @Test
    void lockUser_shouldRejectAnotherAdmin() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AdminUserService service = new AdminUserService(users, Mockito.mock(AccountRepository.class), Mockito.mock(AuditLogService.class));
        UserEntity target = user(8L, UserRole.ADMIN, UserStatus.ACTIVE);
        when(users.findById(8L)).thenReturn(Optional.of(target));

        assertThrows(AdminUserOperationForbiddenException.class, () -> service.lockUser(7L, 8L, "reason"));
        verify(users, never()).save(any());
    }

    @Test
    void statusChange_shouldRejectUnknownUser() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AdminUserService service = new AdminUserService(users, Mockito.mock(AccountRepository.class), Mockito.mock(AuditLogService.class));
        when(users.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.unlockUser(7L, 99L, "reason"));
    }

    private UserEntity user(Long id, UserRole role, UserStatus status) {
        UserEntity user = new UserEntity("user" + id + "@example.com", "secret-hash", "User " + id, role, status);
        setField(user, "id", id);
        return user;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
