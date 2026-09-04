package com.chethu.paymentledgerservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.NotificationEventType;

public record FinancialNotificationEvent(
        Long targetUserId,
        NotificationEventType eventType,
        String transactionReference,
        BigDecimal amount,
        LocalDateTime createdAt,
        String relatedAccountDisplay) {
}
