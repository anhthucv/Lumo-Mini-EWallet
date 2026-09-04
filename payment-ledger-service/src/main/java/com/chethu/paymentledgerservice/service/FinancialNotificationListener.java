package com.chethu.paymentledgerservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Component
public class FinancialNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(FinancialNotificationListener.class);

    private final UserRepository userRepository;
    private final EmailService emailService;

    public FinancialNotificationListener(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FinancialNotificationEvent event) {
        if (event.eventType() != NotificationEventType.DEPOSIT_SUCCESS
                && event.eventType() != NotificationEventType.TRANSFER_RECEIVED) {
            return;
        }
        if (event.targetUserId() == null) {
            log.warn("Skipping financial notification without target user: eventType={}, reference={}",
                    event.eventType(), event.transactionReference());
            return;
        }

        try {
            UserEntity user = userRepository.findById(event.targetUserId()).orElse(null);
            if (user == null) {
                log.warn("Skipping financial notification for missing user: userId={}, eventType={}, reference={}",
                        event.targetUserId(), event.eventType(), event.transactionReference());
                return;
            }
            emailService.sendFinancialNotification(user.getEmail(), event);
        } catch (Exception ex) {
            log.error("Financial notification delivery failed: userId={}, eventType={}, reference={}",
                    event.targetUserId(), event.eventType(), event.transactionReference(), ex);
        }
    }
}
