package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;

class FinancialNotificationPersistenceListenerTest {
    @Test
    void listenerRunsAfterCommit() throws Exception {
        TransactionalEventListener annotation = FinancialNotificationPersistenceListener.class
                .getMethod("handle", FinancialNotificationEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }

    @Test
    void persistenceFailureDoesNotEscapeAfterCommitBoundary() {
        NotificationPersistenceService persistence = org.mockito.Mockito.mock(NotificationPersistenceService.class);
        FinancialNotificationEvent event = new FinancialNotificationEvent(42L,
                NotificationEventType.DEPOSIT_SUCCESS, "DEPOSIT-1", new BigDecimal("1000.00"),
                LocalDateTime.now(), null);
        doThrow(new RuntimeException("database unavailable")).when(persistence).persist(event);
        FinancialNotificationPersistenceListener listener = new FinancialNotificationPersistenceListener(persistence);

        assertDoesNotThrow(() -> listener.handle(event));
        verify(persistence).persist(event);
    }

    @Test
    void legacyOutgoingEventIsIgnored() {
        NotificationPersistenceService persistence = org.mockito.Mockito.mock(NotificationPersistenceService.class);
        FinancialNotificationPersistenceListener listener = new FinancialNotificationPersistenceListener(persistence);

        listener.handle(new FinancialNotificationEvent(42L, NotificationEventType.TRANSFER_SENT, "REF-1",
                new BigDecimal("1000.00"), LocalDateTime.now(), null));

        verify(persistence, never()).persist(any());
    }
}
