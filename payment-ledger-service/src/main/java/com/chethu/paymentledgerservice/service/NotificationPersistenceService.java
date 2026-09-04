package com.chethu.paymentledgerservice.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.dto.NotificationResponse;
import com.chethu.paymentledgerservice.dto.UnreadCountResponse;
import com.chethu.paymentledgerservice.dto.MarkAllNotificationsReadResponse;
import com.chethu.paymentledgerservice.entity.NotificationEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;
import com.chethu.paymentledgerservice.exception.NotificationNotFoundException;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.NotificationRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class NotificationPersistenceService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationPersistenceService(NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(FinancialNotificationEvent event) {
        UserEntity user = findUser(event.targetUserId());
        NotificationContent content = NotificationContent.from(event);
        notificationRepository.save(new NotificationEntity(
                user,
                event.eventType(),
                content.title(),
                content.message(),
                event.amount(),
                event.transactionReference(),
                event.createdAt()));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findForUser(Long userId, Pageable pageable) {
        UserEntity user = findUser(userId);
        return notificationRepository.findAllByUserOrderByCreatedAtDescIdDesc(user, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCountForUser(Long userId) {
        UserEntity user = findUser(userId);
        return new UnreadCountResponse(notificationRepository.countByUserAndReadAtIsNull(user));
    }

    @Transactional
    public NotificationResponse markReadForUser(Long userId, Long notificationId) {
        UserEntity user = findUser(userId);
        NotificationEntity notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllReadForUser(Long userId) {
        UserEntity user = findUser(userId);
        int updatedCount = notificationRepository.markAllUnreadAsRead(user, LocalDateTime.now());
        return new MarkAllNotificationsReadResponse(updatedCount);
    }

    private UserEntity findUser(Long userId) {
        if (userId == null) {
            throw new UserNotFoundException(null);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private record NotificationContent(String title, String message) {
        private static NotificationContent from(FinancialNotificationEvent event) {
            String amount = event.amount().toPlainString();
            return switch (event.eventType()) {
                case DEPOSIT_SUCCESS -> new NotificationContent(
                        "Money added", "Your wallet received " + amount + ".");
                case WITHDRAW_SUCCESS -> new NotificationContent(
                        "Withdrawal successful", "You withdrew " + amount + " from your wallet.");
                case TRANSFER_SENT -> new NotificationContent(
                        "Transfer successful", "You sent " + amount + " to " + relatedAccount(event) + ".");
                case TRANSFER_RECEIVED -> new NotificationContent(
                        "Money received", "You received " + amount + " from " + relatedAccount(event) + ".");
            };
        }

        private static String relatedAccount(FinancialNotificationEvent event) {
            return event.relatedAccountDisplay() == null ? "the related account" : event.relatedAccountDisplay();
        }
    }
}
