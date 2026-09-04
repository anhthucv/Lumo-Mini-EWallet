package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.chethu.paymentledgerservice.domain.NotificationEventType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.event.FinancialNotificationEvent;

class NotificationEventServiceTest {
    @Test
    void publishTransferCreatesEventForCorrectTargetAndMaskedRelatedAccount() throws Exception {
        ApplicationEventPublisher publisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        NotificationEventService service = new NotificationEventService(publisher);
        AccountEntity sender = account("ACC-SENDER", 10L);
        AccountEntity recipient = account("ACC-RECIPIENT", 20L);
        UserEntity recipientUser = new UserEntity("recipient@example.com", "hash", "Recipient",
                com.chethu.paymentledgerservice.domain.UserRole.USER,
                com.chethu.paymentledgerservice.domain.UserStatus.ACTIVE);
        setId(recipientUser, 200L);
        recipient.assignUser(recipientUser);

        service.publishTransferSent(sender, recipient, new BigDecimal("125000.00"), "TRANSFER-123");

        org.mockito.ArgumentCaptor<FinancialNotificationEvent> captor =
                org.mockito.ArgumentCaptor.forClass(FinancialNotificationEvent.class);
        verify(publisher).publishEvent(captor.capture());
        FinancialNotificationEvent event = captor.getValue();
        assertEquals(10L, event.targetUserId());
        assertEquals(NotificationEventType.TRANSFER_SENT, event.eventType());
        assertEquals(new BigDecimal("125000.00"), event.amount());
        assertEquals("TRANSFER-123", event.transactionReference());
        assertEquals("****IENT", event.relatedAccountDisplay());
    }

    @Test
    void publishDepositUsesDepositEventType() {
        ApplicationEventPublisher publisher = event -> assertInstanceOf(FinancialNotificationEvent.class, event);
        NotificationEventService service = new NotificationEventService(publisher);
        AccountEntity account = account("ACC-1234", 10L);

        service.publishDepositSuccess(account, new BigDecimal("1000.00"), "DEPOSIT-1");
    }

    private AccountEntity account(String number, Long userId) throws RuntimeException {
        AccountEntity account = new AccountEntity(number, "Owner");
        UserEntity user = new UserEntity("user" + userId + "@example.com", "hash", "Owner",
                com.chethu.paymentledgerservice.domain.UserRole.USER,
                com.chethu.paymentledgerservice.domain.UserStatus.ACTIVE);
        try {
            setId(user, userId);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        account.assignUser(user);
        return account;
    }

    private void setId(UserEntity user, Long id) throws Exception {
        Field field = UserEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
