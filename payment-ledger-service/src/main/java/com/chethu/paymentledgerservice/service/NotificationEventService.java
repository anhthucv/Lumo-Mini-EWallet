package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;

@Service
public class NotificationEventService {
    private final ApplicationEventPublisher eventPublisher;

    public NotificationEventService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishDepositSuccess(AccountEntity account, BigDecimal amount, String transactionReference) {
        publish(account, NotificationEventType.DEPOSIT_SUCCESS, amount, transactionReference, null);
    }

    public void publishWithdrawSuccess(AccountEntity account, BigDecimal amount, String transactionReference) {
        publish(account, NotificationEventType.WITHDRAW_SUCCESS, amount, transactionReference, null);
    }

    public void publishTransferSent(AccountEntity sender, AccountEntity recipient, BigDecimal amount,
            String transactionReference) {
        publish(sender, NotificationEventType.TRANSFER_SENT, amount, transactionReference,
                displayAccount(recipient));
    }

    public void publishTransferReceived(AccountEntity recipient, AccountEntity sender, BigDecimal amount,
            String transactionReference) {
        publish(recipient, NotificationEventType.TRANSFER_RECEIVED, amount, transactionReference,
                displayAccount(sender));
    }

    private void publish(AccountEntity account, NotificationEventType eventType, BigDecimal amount,
            String transactionReference, String relatedAccountDisplay) {
        Long targetUserId = account.getUser() == null ? null : account.getUser().getId();
        eventPublisher.publishEvent(new FinancialNotificationEvent(
                targetUserId,
                eventType,
                transactionReference,
                amount,
                LocalDateTime.now(),
                relatedAccountDisplay));
    }

    private String displayAccount(AccountEntity account) {
        String accountNumber = account.getAccountNumber();
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
