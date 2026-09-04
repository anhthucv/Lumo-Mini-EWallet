package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.RiskReasonCode;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.exception.RiskRejectedException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;

class AccountRiskControlsTest {
    @Test
    void flaggedDeposit_shouldCompleteAndPersistFlagAudit() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgers = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journals = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionService transactions = org.mockito.Mockito.mock(TransactionService.class);
        IdempotencyService idempotency = org.mockito.Mockito.mock(IdempotencyService.class);
        RiskEvaluationService riskEvaluation = org.mockito.Mockito.mock(RiskEvaluationService.class);
        RiskAuditService audit = org.mockito.Mockito.mock(RiskAuditService.class);
        AccountEntity account = account(42L, "0.00");
        RiskEvaluationResult flagged = new RiskEvaluationResult(RiskDecision.FLAG,
                List.of(RiskReasonCode.LARGE_AMOUNT));
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accounts.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(idempotency.findExisting(any(), any(), any(), any(), any())).thenReturn(null);
        when(riskEvaluation.evaluate(account, LimitOperationType.DEPOSIT, new BigDecimal("10000000.00")))
                .thenReturn(flagged);
        when(ledgers.findByWalletAccount(account)).thenReturn(Optional.of(wallet(account)));
        when(ledgers.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(system()));
        when(journals.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accounts.save(account)).thenReturn(account);

        AccountService service = new AccountService(accounts, transactions,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgers, journals, idempotency,
                org.mockito.Mockito.mock(TransactionLimitService.class), riskEvaluation, audit);
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("10000000.00"));

        service.depositForCurrentUser(42L, request, "flag-key");

        assertEquals(new BigDecimal("10000000.00"), account.getBalance());
        verify(audit).recordFlagged(account, LimitOperationType.DEPOSIT, request.getAmount(), flagged);
        verify(idempotency).saveCompleted(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectedWithdraw_shouldAuditAndCreateNoFinancialSideEffects() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgers = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journals = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionService transactions = org.mockito.Mockito.mock(TransactionService.class);
        IdempotencyService idempotency = org.mockito.Mockito.mock(IdempotencyService.class);
        RiskEvaluationService riskEvaluation = org.mockito.Mockito.mock(RiskEvaluationService.class);
        RiskAuditService audit = org.mockito.Mockito.mock(RiskAuditService.class);
        AccountEntity account = account(42L, "200000.00");
        RiskEvaluationResult rejected = new RiskEvaluationResult(RiskDecision.REJECT,
                List.of(RiskReasonCode.RAPID_OUTGOING_ACTIVITY));
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accounts.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(idempotency.findExisting(any(), any(), any(), any(), any())).thenReturn(null);
        when(riskEvaluation.evaluate(account, LimitOperationType.WITHDRAW, new BigDecimal("100.00")))
                .thenReturn(rejected);

        AccountService service = new AccountService(accounts, transactions,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgers, journals, idempotency,
                org.mockito.Mockito.mock(TransactionLimitService.class), riskEvaluation, audit);
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("100.00"));

        assertThrows(RiskRejectedException.class,
                () -> service.withdrawForCurrentUser(42L, request, "reject-key"));

        assertEquals(new BigDecimal("200000.00"), account.getBalance());
        verify(audit).recordRejected(account, LimitOperationType.WITHDRAW, request.getAmount(), rejected);
        verify(journals, never()).save(any(JournalEntity.class));
        verify(transactions, never()).recordTransaction(any(), any(), any(), any(), any());
        verify(idempotency, never()).saveCompleted(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectedAudit_shouldUseIndependentTransaction() throws NoSuchMethodException {
        org.springframework.transaction.annotation.Transactional annotation = RiskAuditService.class
                .getMethod("recordRejected", AccountEntity.class, LimitOperationType.class,
                        BigDecimal.class, RiskEvaluationResult.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, annotation.propagation());
    }

    private AccountEntity account(Long id, String balance) {
        AccountEntity account = new AccountEntity("ACC-RISK-" + id, "Risk Owner");
        try {
            java.lang.reflect.Field field = AccountEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        account.deposit(new BigDecimal(balance));
        return account;
    }

    private LedgerAccountEntity wallet(AccountEntity account) {
        return new LedgerAccountEntity("WALLET-" + account.getAccountNumber(), LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account);
    }

    private LedgerAccountEntity system() {
        return new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                AccountClass.ASSET, null);
    }
}
