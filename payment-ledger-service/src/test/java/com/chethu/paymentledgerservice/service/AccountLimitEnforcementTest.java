package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.PerTransactionLimitExceededException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;

class AccountLimitEnforcementTest {
    @Test
    void limitRejectionOccursAfterLockAndBeforeFinancialPosting() {
        AccountRepository accounts = mock(AccountRepository.class);
        TransactionLimitService limits = mock(TransactionLimitService.class);
        AccountEntity account = new AccountEntity("ACC-LIMIT", "Limit Owner");
        setId(account, 1L);
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        org.mockito.Mockito.doThrow(new PerTransactionLimitExceededException(new BigDecimal("50.00")))
                .when(limits).validate(eq(account), eq(LimitOperationType.DEPOSIT), eq(new BigDecimal("51.00")));

        AccountService service = new AccountService(accounts, mock(TransactionService.class),
                mock(AccountNumberGenerator.class), mock(LedgerAccountRepository.class), mock(JournalRepository.class),
                mock(IdempotencyService.class), limits,
                mock(RiskEvaluationService.class, invocation -> new RiskEvaluationResult(
                        com.chethu.paymentledgerservice.domain.RiskDecision.ALLOW, java.util.List.of())),
                mock(RiskAuditService.class), mock(NotificationEventService.class));
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("51.00"));

        assertThrows(PerTransactionLimitExceededException.class,
                () -> service.depositForCurrentUser(42L, request, "limit-key"));

        verify(accounts).findByIdForUpdate(1L);
        verify(accounts, never()).save(any(AccountEntity.class));
    }

    private void setId(AccountEntity account, Long id) {
        try {
            var field = AccountEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
