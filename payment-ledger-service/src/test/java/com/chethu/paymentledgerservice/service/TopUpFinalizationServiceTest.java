package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.InvalidPaymentWebhookException;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;
import com.chethu.paymentledgerservice.payment.provider.VerifiedPaymentWebhook;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

class TopUpFinalizationServiceTest {
    @Test
    void successfulWebhookCreditsOnceAndPostsBalancedProviderClearingJournal() {
        Fixture fixture = fixture();

        fixture.service.finalizeVerifiedWebhook(webhook());

        assertEquals(new BigDecimal("110000.00"), fixture.account.getBalance());
        assertEquals(TopUpPaymentStatus.SUCCESS, fixture.payment.getStatus());
        assertNotNull(fixture.payment.getCompletedAt());
        assertEquals("payos-ref-77", fixture.payment.getProviderReference());
        assertEquals(fixture.payment.getJournal(), fixture.payment.getTransaction().getJournal());
        assertEquals(0, fixture.payment.getAmount().compareTo(new BigDecimal("10000.00")));
        JournalEntity journal = fixture.payment.getJournal();
        assertEquals(2, journal.getEntries().size());
        assertEquals(LedgerEntryType.DEBIT, journal.getEntries().get(0).getEntryType());
        assertEquals(LedgerAccountType.PROVIDER_CLEARING,
                journal.getEntries().get(0).getLedgerAccount().getType());
        assertEquals(LedgerEntryType.CREDIT, journal.getEntries().get(1).getEntryType());
        assertEquals(LedgerAccountType.WALLET, journal.getEntries().get(1).getLedgerAccount().getType());
        assertEquals(new BigDecimal("10000.00"), journal.getEntries().get(0).getAmount());
        assertEquals(new BigDecimal("10000.00"), journal.getEntries().get(1).getAmount());
        verify(fixture.transactionRepository).save(any(TransactionEntity.class));
        verify(fixture.journalRepository).save(journal);
        verify(fixture.notificationEventService).publishDepositSuccess(
                fixture.account, new BigDecimal("10000.00"), journal.getReference());
    }

    @Test
    void duplicateSuccessfulWebhookDoesNotRepeatFinancialEffects() {
        Fixture fixture = fixture();
        fixture.service.finalizeVerifiedWebhook(webhook());
        BigDecimal balanceAfterFirst = fixture.account.getBalance();

        fixture.service.finalizeVerifiedWebhook(webhook());

        assertEquals(balanceAfterFirst, fixture.account.getBalance());
        verify(fixture.transactionRepository, times(1)).save(any(TransactionEntity.class));
        verify(fixture.journalRepository, times(1)).save(any(JournalEntity.class));
        verify(fixture.notificationEventService, times(1)).publishDepositSuccess(
                any(), any(), any());
    }

    @Test
    void amountMismatchDoesNotMutateWallet() {
        Fixture fixture = fixture();

        assertThrows(InvalidPaymentWebhookException.class, () -> fixture.service.finalizeVerifiedWebhook(
                new VerifiedPaymentWebhook(PaymentProviderType.PAYOS, 77L, new BigDecimal("9000.00"),
                        "VND", "payos-ref-77", "tx-77", "00", true)));

        assertEquals(new BigDecimal("100000.00"), fixture.account.getBalance());
        verify(fixture.transactionRepository, never()).save(any());
        verify(fixture.journalRepository, never()).save(any());
    }

    @Test
    void currencyMismatchDoesNotMutateWallet() {
        Fixture fixture = fixture();

        assertThrows(InvalidPaymentWebhookException.class, () -> fixture.service.finalizeVerifiedWebhook(
                new VerifiedPaymentWebhook(PaymentProviderType.PAYOS, 77L, new BigDecimal("10000.00"),
                        "USD", "payos-ref-77", "tx-77", "00", true)));

        assertEquals(new BigDecimal("100000.00"), fixture.account.getBalance());
        verify(fixture.transactionRepository, never()).save(any());
    }

    @Test
    void unknownVerifiedOrderDoesNotCreatePaymentOrAccountingData() {
        Fixture fixture = fixture();
        when(fixture.topUps.findByMerchantOrderCodeForUpdate(77L)).thenReturn(Optional.empty());

        fixture.service.finalizeVerifiedWebhook(webhook());

        verify(fixture.accounts, never()).findByIdForUpdate(any());
        verify(fixture.transactionRepository, never()).save(any());
        verify(fixture.journalRepository, never()).save(any());
        verify(fixture.notificationEventService, never()).publishDepositSuccess(any(), any(), any());
    }

    @Test
    void nonSuccessVerifiedWebhookIsAcknowledgedWithoutMutation() {
        Fixture fixture = fixture();

        fixture.service.finalizeVerifiedWebhook(new VerifiedPaymentWebhook(
                PaymentProviderType.PAYOS, 77L, new BigDecimal("10000.00"), "VND",
                "payos-ref-77", "tx-77", "01", false));

        verify(fixture.topUps, never()).findByMerchantOrderCodeForUpdate(any());
        assertEquals(new BigDecimal("100000.00"), fixture.account.getBalance());
    }

    @Test
    void providerReferenceMismatchDoesNotMutateWallet() {
        Fixture fixture = fixture();

        assertThrows(InvalidPaymentWebhookException.class, () -> fixture.service.finalizeVerifiedWebhook(
                new VerifiedPaymentWebhook(PaymentProviderType.PAYOS, 77L, new BigDecimal("10000.00"),
                        "VND", "other-payment-link", "tx-77", "00", true)));

        assertEquals(new BigDecimal("100000.00"), fixture.account.getBalance());
        verify(fixture.transactionRepository, never()).save(any());
    }

    @Test
    void ledgerPostingFailureDoesNotLeaveFinalizedTopUpOrWalletMutation() {
        Fixture fixture = fixture();
        when(fixture.ledgers.findByCode("PROVIDER_CLEARING")).thenThrow(
                new IllegalStateException("ledger unavailable"));

        assertThrows(IllegalStateException.class, () -> fixture.service.finalizeVerifiedWebhook(webhook()));

        assertEquals(new BigDecimal("100000.00"), fixture.account.getBalance());
        assertEquals(TopUpPaymentStatus.PENDING, fixture.payment.getStatus());
        verify(fixture.transactionRepository, never()).save(any());
        verify(fixture.journalRepository, never()).save(any());
        verify(fixture.notificationEventService, never()).publishDepositSuccess(any(), any(), any());
    }

    private Fixture fixture() {
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TopUpPaymentRepository topUps = Mockito.mock(TopUpPaymentRepository.class);
        LedgerAccountRepository ledgers = Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journals = Mockito.mock(JournalRepository.class);
        TransactionRepository transactions = Mockito.mock(TransactionRepository.class);
        NotificationEventService notifications = Mockito.mock(NotificationEventService.class);
        AccountEntity account = account(1L, "100000.00");
        TopUpPaymentEntity payment = payment(account);
        JournalEntity journal = new JournalEntity("TOPUP-77");
        TransactionService transactionService = new TransactionService(transactions, accounts);

        when(topUps.findByMerchantOrderCodeForUpdate(77L)).thenReturn(Optional.of(payment));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(ledgers.findByWalletAccount(account)).thenReturn(Optional.of(
                new LedgerAccountEntity("WALLET-ACC", LedgerAccountType.WALLET, AccountClass.LIABILITY, account)));
        when(ledgers.findByCode("PROVIDER_CLEARING")).thenReturn(Optional.of(
                new LedgerAccountEntity("PROVIDER_CLEARING", LedgerAccountType.PROVIDER_CLEARING,
                        AccountClass.ASSET, null)));
        when(journals.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accounts.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactions.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new Fixture(accounts, topUps, ledgers, journals, transactions, notifications,
                account, payment,
                new TopUpFinalizationService(topUps, accounts, ledgers, journals, transactionService, notifications));
    }

    private TopUpPaymentEntity payment(AccountEntity account) {
        TopUpPaymentEntity payment = new TopUpPaymentEntity(account, new BigDecimal("10000.00"), "key-77");
        setField(payment, "id", 77L);
        payment.assignMerchantOrderCode();
        payment.attachCheckout(new PaymentCheckoutResult(PaymentProviderType.PAYOS, 77L,
                "payos-ref-77", "https://payos.test/77"));
        return payment;
    }

    private AccountEntity account(Long id, String balance) {
        AccountEntity account = new AccountEntity("ACC-" + id, "Top-up Owner");
        setField(account, "id", id);
        account.deposit(new BigDecimal(balance));
        return account;
    }

    private VerifiedPaymentWebhook webhook() {
        return new VerifiedPaymentWebhook(PaymentProviderType.PAYOS, 77L, new BigDecimal("10000.00"),
                "VND", "payos-ref-77", "tx-77", "00", true);
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private record Fixture(AccountRepository accounts, TopUpPaymentRepository topUps,
            LedgerAccountRepository ledgers, JournalRepository journalRepository,
            TransactionRepository transactionRepository, NotificationEventService notificationEventService,
            AccountEntity account, TopUpPaymentEntity payment, TopUpFinalizationService service) {
    }
}
