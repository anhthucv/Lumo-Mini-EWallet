package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.dto.MarkAllNotificationsReadResponse;
import com.chethu.paymentledgerservice.dto.NotificationResponse;
import com.chethu.paymentledgerservice.entity.NotificationEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;
import com.chethu.paymentledgerservice.exception.NotificationNotFoundException;
import com.chethu.paymentledgerservice.repository.NotificationRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

class NotificationPersistenceServiceTest {
    @Test
    void persistMapsDepositEventToUnreadNotificationForTargetUser() {
        NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        UserEntity user = user(42L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        NotificationPersistenceService service = new NotificationPersistenceService(notifications, users);
        FinancialNotificationEvent event = event(42L, NotificationEventType.DEPOSIT_SUCCESS, null);

        service.persist(event);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notifications).save(captor.capture());
        NotificationEntity notification = captor.getValue();
        assertEquals(user, notification.getUser());
        assertEquals(NotificationEventType.DEPOSIT_SUCCESS, notification.getType());
        assertEquals("Money added", notification.getTitle());
        assertEquals("Your wallet received 125000.00.", notification.getMessage());
        assertEquals(new BigDecimal("125000.00"), notification.getAmount());
        assertEquals("DEPOSIT-1", notification.getTransactionReference());
        assertNull(notification.getReadAt());
        assertEquals("Money added", NotificationResponse.from(notification).title());
    }

    @Test
    void listAndUnreadCountAreScopedToUserAndUsePageableRepositoryQueries() {
        NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        UserEntity user = user(42L);
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(notifications.findAllByUserOrderByCreatedAtDescIdDesc(user, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of()));
        when(notifications.countByUserAndReadAtIsNull(user)).thenReturn(3L);
        NotificationPersistenceService service = new NotificationPersistenceService(notifications, users);

        assertEquals(0, service.findForUser(42L, pageable).getTotalElements());
        assertEquals(3L, service.unreadCountForUser(42L).unreadCount());
        verify(notifications).findAllByUserOrderByCreatedAtDescIdDesc(user, pageable);
        verify(notifications).countByUserAndReadAtIsNull(user);
    }

    @Test
    void markReadIsIdempotentAndMarkAllReturnsBulkUpdateCount() {
        NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        UserEntity user = user(42L);
        NotificationEntity notification = new NotificationEntity(user, NotificationEventType.TRANSFER_SENT,
                "Transfer successful", "You sent 1000.00 to ****1234.", new BigDecimal("1000.00"),
                "TRANSFER-1", LocalDateTime.now());
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(notifications.findByIdAndUser(7L, user)).thenReturn(Optional.of(notification));
        when(notifications.markAllUnreadAsRead(any(), any())).thenReturn(4);
        NotificationPersistenceService service = new NotificationPersistenceService(notifications, users);

        NotificationResponse first = service.markReadForUser(42L, 7L);
        NotificationResponse second = service.markReadForUser(42L, 7L);
        MarkAllNotificationsReadResponse all = service.markAllReadForUser(42L);

        assertEquals(true, first.read());
        assertEquals(first.readAt(), second.readAt());
        assertEquals(4, all.updatedCount());
        verify(notifications).markAllUnreadAsRead(any(), any());
    }

    @Test
    void markReadDoesNotFindNotificationOwnedByAnotherUser() {
        NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        UserEntity user = user(42L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(notifications.findByIdAndUser(7L, user)).thenReturn(Optional.empty());
        NotificationPersistenceService service = new NotificationPersistenceService(notifications, users);

        org.junit.jupiter.api.Assertions.assertThrows(NotificationNotFoundException.class,
                () -> service.markReadForUser(42L, 7L));
        verify(notifications, never()).save(any());
    }

    @Test
    void persistUsesRequiresNewTransaction() throws Exception {
        Transactional transactional = NotificationPersistenceService.class
                .getMethod("persist", FinancialNotificationEvent.class)
                .getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    private FinancialNotificationEvent event(Long userId, NotificationEventType type, String related) {
        return new FinancialNotificationEvent(userId, type, "DEPOSIT-1", new BigDecimal("125000.00"),
                LocalDateTime.now(), related);
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity("user" + id + "@example.com", "hash", "User",
                com.chethu.paymentledgerservice.domain.UserRole.USER,
                com.chethu.paymentledgerservice.domain.UserStatus.ACTIVE);
        try {
            java.lang.reflect.Field field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return user;
    }
}
