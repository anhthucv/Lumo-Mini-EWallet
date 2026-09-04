package com.chethu.paymentledgerservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;

@Component
public class FinancialNotificationPersistenceListener {
    private static final Logger log = LoggerFactory.getLogger(FinancialNotificationPersistenceListener.class);

    private final NotificationPersistenceService notificationPersistenceService;

    public FinancialNotificationPersistenceListener(NotificationPersistenceService notificationPersistenceService) {
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FinancialNotificationEvent event) {
        if (event.eventType() != NotificationEventType.DEPOSIT_SUCCESS
                && event.eventType() != NotificationEventType.TRANSFER_RECEIVED) {
            return;
        }
        try {
            notificationPersistenceService.persist(event);
        } catch (Exception ex) {
            log.error("In-app notification persistence failed: userId={}, eventType={}, reference={}",
                    event.targetUserId(), event.eventType(), event.transactionReference(), ex);
        }
    }
}
