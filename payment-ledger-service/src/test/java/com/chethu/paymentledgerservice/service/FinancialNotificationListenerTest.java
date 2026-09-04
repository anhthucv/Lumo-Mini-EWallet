package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;
import com.chethu.paymentledgerservice.repository.UserRepository;

class FinancialNotificationListenerTest {
    @Test
    void handlerIsConfiguredForAfterCommit() throws Exception {
        TransactionalEventListener annotation = FinancialNotificationListener.class
                .getMethod("handle", FinancialNotificationEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        org.junit.jupiter.api.Assertions.assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }

    @Test
    void mailFailureDoesNotEscapeListener() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
        UserEntity user = new UserEntity("user@example.com", "hash", "User",
                com.chethu.paymentledgerservice.domain.UserRole.USER,
                com.chethu.paymentledgerservice.domain.UserStatus.ACTIVE);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("mail unavailable")).when(emailService)
                .sendFinancialNotification(any(), any());
        FinancialNotificationListener listener = new FinancialNotificationListener(users, emailService);

        assertDoesNotThrow(() -> listener.handle(event(42L, NotificationEventType.WITHDRAW_SUCCESS)));
        verify(emailService).sendFinancialNotification(eq("user@example.com"), any(FinancialNotificationEvent.class));
    }

    @Test
    void missingTargetUserDoesNotSendMail() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
        when(users.findById(42L)).thenReturn(Optional.empty());
        FinancialNotificationListener listener = new FinancialNotificationListener(users, emailService);

        listener.handle(event(42L, NotificationEventType.TRANSFER_RECEIVED));

        verify(emailService, never()).sendFinancialNotification(any(), any());
    }

    private FinancialNotificationEvent event(Long userId, NotificationEventType type) {
        return new FinancialNotificationEvent(userId, type, "REF-1", new BigDecimal("1000.00"),
                LocalDateTime.now(), null);
    }

}
