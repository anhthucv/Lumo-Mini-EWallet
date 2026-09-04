package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.entity.NotificationEntity;

public record NotificationResponse(
        Long id,
        NotificationEventType type,
        String title,
        String message,
        BigDecimal amount,
        String transactionReference,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt) {
    public static NotificationResponse from(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getAmount(),
                notification.getTransactionReference(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
