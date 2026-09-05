package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.PayoutEntity;
import com.chethu.paymentledgerservice.exception.IdempotencyConflictException;
import com.chethu.paymentledgerservice.exception.InsufficientBalanceException;
import com.chethu.paymentledgerservice.exception.PerTransactionLimitExceededException;
import com.chethu.paymentledgerservice.exception.RiskRejectedException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.PayoutRepository;

class PayoutPersistenceServiceTest {
    @Test
    void reservesFundsAndCreatesPendingPayout() {
        Fixture fixture = fixture(new BigDecimal("100000.00"));

        PayoutEntity payout = fixture.service.reserve(42L, money("30000"), "970422", "****6789",
                "hash", "encrypted", "key-1");

        assertEquals(new BigDecimal("70000.00"), fixture.account.getBalance());
        assertEquals("PENDING", payout.getStatus().name());
        verify(fixture.accounts).save(fixture.account);
        verify(fixture.payouts).save(any(PayoutEntity.class));
    }

    @Test
    void reservationJournalIsBalancedWithWalletDebitAndPendingCredit() {
        Fixture fixture = fixture(new BigDecimal("100000.00"));

        PayoutEntity payout = fixture.service.reserve(42L, money("30000"), "970422", "****6789",
                "hash", "encrypted", "key-1");
        JournalEntity journal = payout.getReservationJournal();

        assertEquals(2, journal.getEntries().size());
        assertEquals(LedgerEntryType.DEBIT, journal.getEntries().get(0).getEntryType());
        assertEquals(LedgerAccountType.WALLET, journal.getEntries().get(0).getLedgerAccount().getType());
        assertEquals(LedgerEntryType.CREDIT, journal.getEntries().get(1).getEntryType());
        assertEquals(LedgerAccountType.PAYOUT_PENDING, journal.getEntries().get(1).getLedgerAccount().getType());
        assertEquals(money("30000"), journal.getEntries().get(0).getAmount());
        assertEquals(money("30000"), journal.getEntries().get(1).getAmount());
        assertEquals(true, journal.isBalanced());
    }

    @Test
    void retainedMinimumBalanceIsAllowedExactly() {
        Fixture fixture = fixture(new BigDecimal("80000.00"));

        fixture.service.reserve(42L, money("30000"), "970422", "****6789", "hash", "encrypted", "key-1");

        assertEquals(new BigDecimal("50000.00"), fixture.account.getBalance());
    }

    @Test
    void insufficientBalanceIsRejectedWithoutReservation() {
        Fixture fixture = fixture(new BigDecimal("79999.99"));

        assertThrows(InsufficientBalanceException.class, () -> fixture.service.reserve(42L, money("30000"),
                "970422", "****6789", "hash", "encrypted", "key-1"));
        verify(fixture.journals, never()).save(any());
        verify(fixture.payouts, never()).save(any());
    }

    @Test
    void withdrawalLimitIsReusedBeforeReservation() {
        Fixture fixture = fixture(new BigDecimal("100000.00"));
        doThrow(new PerTransactionLimitExceededException(money("20000"))).when(fixture.limits)
                .validate(eq(fixture.account), eq(LimitOperationType.WITHDRAW), eq(money("30000")));

        assertThrows(PerTransactionLimitExceededException.class, () -> fixture.service.reserve(42L, money("30000"),
                "970422", "****6789", "hash", "encrypted", "key-1"));
        verify(fixture.journals, never()).save(any());
    }

    @Test
    void rejectedOutgoingRiskStopsReservation() {
        Fixture fixture = fixture(new BigDecimal("100000.00"));
        when(fixture.risk.evaluate(any(), eq(LimitOperationType.WITHDRAW), any()))
                .thenReturn(new RiskEvaluationResult(RiskDecision.REJECT, java.util.List.of()));

        assertThrows(RiskRejectedException.class, () -> fixture.service.reserve(42L, money("30000"),
                "970422", "****6789", "hash", "encrypted", "key-1"));
        verify(fixture.payouts, never()).save(any());
        verify(fixture.audit).recordRejected(eq(fixture.account), eq(LimitOperationType.WITHDRAW),
                eq(money("30000")), any());
    }

    @Test
    void sameIdempotencyPayloadReturnsExistingWithoutSecondReservation() {
        Fixture fixture = fixture(new BigDecimal("100000.00"));
        PayoutEntity existing = new PayoutEntity(fixture.account, money("30000"), "PAYOUT-1", "970422",
                "****6789", "hash", "encrypted", "key-1", new JournalEntity("PAYOUT-RESERVE-1"));
        when(fixture.payouts.findByAccountAndIdempotencyKeyForUpdate(fixture.account, "key-1"))
                .thenReturn(Optional.of(existing));

        assertEquals(existing, fixture.service.reserve(42L, money("30000"), "970422", "****6789",
                "hash", "encrypted", "key-1"));
        verify(fixture.journals, never()).save(any());
        verify(fixture.accounts, never()).save(any());
    }

    @Test
    void sameIdempotencyKeyWithDifferentDestinationConflicts() {
        Fixture fixture = fixture(new BigDecimal("100000.00"));
        PayoutEntity existing = new PayoutEntity(fixture.account, money("30000"), "PAYOUT-1", "970422",
                "****6789", "original-hash", "encrypted", "key-1", new JournalEntity("PAYOUT-RESERVE-1"));
        when(fixture.payouts.findByAccountAndIdempotencyKeyForUpdate(fixture.account, "key-1"))
                .thenReturn(Optional.of(existing));

        assertThrows(IdempotencyConflictException.class, () -> fixture.service.reserve(42L, money("30000"),
                "970422", "****0000", "different-hash", "encrypted", "key-1"));
        verify(fixture.journals, never()).save(any());
    }

    private Fixture fixture(BigDecimal balance) {
        AccountRepository accounts = mock(AccountRepository.class);
        PayoutRepository payouts = mock(PayoutRepository.class);
        LedgerAccountRepository ledgers = mock(LedgerAccountRepository.class);
        JournalRepository journals = mock(JournalRepository.class);
        TransactionLimitService limits = mock(TransactionLimitService.class);
        RiskEvaluationService risk = mock(RiskEvaluationService.class);
        RiskAuditService audit = mock(RiskAuditService.class);
        AccountEntity account = new AccountEntity("ACC-PAYOUT", "Payout Owner");
        setField(account, "id", 42L);
        account.deposit(balance);
        LedgerAccountEntity wallet = new LedgerAccountEntity("WALLET-ACC-PAYOUT", LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account);
        LedgerAccountEntity pending = new LedgerAccountEntity("PAYOUT_PENDING", LedgerAccountType.PAYOUT_PENDING,
                AccountClass.LIABILITY, null);
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accounts.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(ledgers.findByWalletAccount(account)).thenReturn(Optional.of(wallet));
        when(ledgers.findByCode("PAYOUT_PENDING")).thenReturn(Optional.of(pending));
        when(risk.evaluate(any(), any(), any())).thenReturn(new RiskEvaluationResult(RiskDecision.ALLOW, java.util.List.of()));
        when(journals.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payouts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PayoutPersistenceService service = new PayoutPersistenceService(accounts, payouts, ledgers, journals,
                limits, risk, audit);
        return new Fixture(service, accounts, payouts, journals, limits, risk, audit, account);
    }

    private BigDecimal money(String value) { return new BigDecimal(value).setScale(2); }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private record Fixture(PayoutPersistenceService service, AccountRepository accounts, PayoutRepository payouts,
            JournalRepository journals, TransactionLimitService limits, RiskEvaluationService risk,
            RiskAuditService audit, AccountEntity account) {
    }
}
