package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.config.TransactionLimitProperties;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.DailyTransactionLimitExceededException;
import com.chethu.paymentledgerservice.exception.PerTransactionLimitExceededException;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

class TransactionLimitServiceTest {
    private TransactionRepository transactionRepository;
    private TransactionLimitService service;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        service = new TransactionLimitService(transactionRepository, new TransactionLimitProperties());
        account = new AccountEntity("ACC-LIMIT", "Limit Owner");
        when(transactionRepository.sumAmountForAccountAndTypeAndStatusBetween(any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
    }

    @Test
    void amountBelowPerTransactionLimitIsAllowed() {
        service.validate(account, LimitOperationType.DEPOSIT, new BigDecimal("49999999.99"));
    }

    @Test
    void amountExactlyAtPerTransactionLimitIsAllowed() {
        service.validate(account, LimitOperationType.DEPOSIT, new BigDecimal("50000000.00"));
    }

    @Test
    void amountAbovePerTransactionLimitIsRejected() {
        assertThrows(PerTransactionLimitExceededException.class,
                () -> service.validate(account, LimitOperationType.DEPOSIT, new BigDecimal("50000000.01")));
    }

    @Test
    void dailyUsageBelowLimitIsAllowed() {
        whenUsed("99999999.00", TransactionType.DEPOSIT);
        service.validate(account, LimitOperationType.DEPOSIT, new BigDecimal("1.00"));
    }

    @Test
    void dailyUsageExactlyAtLimitIsAllowed() {
        whenUsed("99999999.00", TransactionType.DEPOSIT);
        service.validate(account, LimitOperationType.DEPOSIT, new BigDecimal("1.00"));
    }

    @Test
    void dailyUsageAboveLimitIsRejected() {
        whenUsed("99999999.00", TransactionType.DEPOSIT);
        assertThrows(DailyTransactionLimitExceededException.class,
                () -> service.validate(account, LimitOperationType.DEPOSIT, new BigDecimal("2.00")));
    }

    @Test
    void noTransactionsTodayReportsZeroUsageAndFullRemainingLimit() {
        var response = service.limitFor(account, LimitOperationType.WITHDRAW);

        assertEquals(new BigDecimal("0"), response.usedToday());
        assertEquals(new BigDecimal("50000000.00"), response.remainingToday());
    }

    @Test
    void transferLimitUsesOnlyTransferOutTransactions() {
        whenUsed("25000000.00", TransactionType.TRANSFER_OUT);
        var response = service.limitFor(account, LimitOperationType.TRANSFER);

        assertEquals(new BigDecimal("25000000.00"), response.usedToday());
        verify(transactionRepository).sumAmountForAccountAndTypeAndStatusBetween(
                eq(account), eq(TransactionType.TRANSFER_OUT), eq(TransactionStatus.SUCCESS), any(LocalDateTime.class),
                any(LocalDateTime.class));
    }

    @Test
    void remainingDailyLimitNeverBecomesNegative() {
        whenUsed("60000000.00", TransactionType.WITHDRAW);

        var response = service.limitFor(account, LimitOperationType.WITHDRAW);

        assertEquals(BigDecimal.ZERO, response.remainingToday());
    }

    private void whenUsed(String amount, TransactionType type) {
        when(transactionRepository.sumAmountForAccountAndTypeAndStatusBetween(
                eq(account), eq(type), eq(TransactionStatus.SUCCESS), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(new BigDecimal(amount));
    }
}
