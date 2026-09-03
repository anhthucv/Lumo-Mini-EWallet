package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotActiveException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

class WithdrawLedgerPostingTest {
    @Test
    void withdraw_shouldCreateBalancedJournalAndLinkedSuccessTransaction() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        AccountEntity account = account("ACC-100", 42L, "200000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        LedgerAccountEntity walletLedger = new LedgerAccountEntity("WALLET-ACC-100", LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account);
        LedgerAccountEntity clearing = new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                AccountClass.ASSET, null);
        when(ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(walletLedger));
        when(ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(clearing));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRepository transactionRepo = transactionRepository;
        TransactionService transactionService = new TransactionService(transactionRepo, accountRepository);
        AccountService service = new AccountService(accountRepository, transactionService,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        AccountResponse response = service.withdrawForCurrentUser(42L, money("100000.00"), null);

        assertEquals(new BigDecimal("100000.00"), response.getBalance());
        ArgumentCaptor<JournalEntity> journalCaptor = ArgumentCaptor.forClass(JournalEntity.class);
        verify(journalRepository).save(journalCaptor.capture());
        JournalEntity journal = journalCaptor.getValue();
        assertEquals(2, journal.getEntries().size());
        LedgerEntryEntity walletEntry = journal.getEntries().get(0);
        LedgerEntryEntity clearingEntry = journal.getEntries().get(1);
        assertEquals(LedgerEntryType.DEBIT, walletEntry.getEntryType());
        assertEquals(LedgerEntryType.CREDIT, clearingEntry.getEntryType());
        assertEquals(new BigDecimal("100000.00"), walletEntry.getAmount());
        assertEquals(new BigDecimal("100000.00"), clearingEntry.getAmount());
        assertEquals(LedgerAccountType.WALLET, walletEntry.getLedgerAccount().getType());
        assertEquals(AccountClass.LIABILITY, walletEntry.getLedgerAccount().getAccountClass());
        assertEquals(LedgerAccountType.SYSTEM_CLEARING, clearingEntry.getLedgerAccount().getType());
        assertEquals(AccountClass.ASSET, clearingEntry.getLedgerAccount().getAccountClass());
        assertEquals(true, journal.isBalanced());

        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        TransactionEntity transaction = transactionCaptor.getValue();
        assertEquals(TransactionType.WITHDRAW, transaction.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals(new BigDecimal("100000.00"), transaction.getAmount());
        assertEquals(new BigDecimal("100000.00"), transaction.getBalanceAfterTransaction());
        assertEquals(journal, transaction.getJournal());
    }

    @Test
    void withdraw_shouldReuseExistingLedgerAccounts() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity account = account("ACC-100", 42L, "300000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(
                new LedgerAccountEntity("WALLET-ACC-100", LedgerAccountType.WALLET,
                        AccountClass.LIABILITY, account)));
        when(ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(
                new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                        AccountClass.ASSET, null)));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        service.withdrawForCurrentUser(42L, money("100000.00"), null);

        verify(ledgerAccountRepository, never()).save(any(LedgerAccountEntity.class));
        assertEquals(new BigDecimal("200000.00"), account.getBalance());
    }

    @Test
    void withdraw_rejectedRulesShouldNotCreateLedgerPosting() {
        for (AccountStatus status : new AccountStatus[] { AccountStatus.FROZEN, AccountStatus.CLOSED }) {
            AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
            LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
            JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
            AccountEntity account = account("ACC-100", 42L, "200000.00");
            setField(account, "status", status);
            when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
            when(accountRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
            AccountService service = new AccountService(accountRepository,
                    org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                    ledgerAccountRepository, journalRepository,
                    new IdempotencyService(org.mockito.Mockito.mock(
                            com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

            assertThrows(AccountNotActiveException.class,
                    () -> service.withdrawForCurrentUser(42L, money("100000.00"), null));
            assertEquals(new BigDecimal("200000.00"), account.getBalance());
            verify(journalRepository, never()).save(any(JournalEntity.class));
        }
    }

    @Test
    void withdraw_insufficientFundsShouldNotCreateLedgerPosting() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity account = account("ACC-100", 42L, "100000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(account));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        assertThrows(RuntimeException.class,
                () -> service.withdrawForCurrentUser(42L, money("60000.00"), null));
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(journalRepository, never()).save(any(JournalEntity.class));
    }

    @Test
    void withdrawForCurrentUser_shouldRemainTransactional() throws Exception {
        assertEquals(Transactional.class,
                AccountService.class.getMethod("withdrawForCurrentUser", Long.class, MoneyOperationRequest.class,
                        String.class)
                        .getAnnotation(Transactional.class).annotationType());
    }

    private MoneyOperationRequest money(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private AccountEntity account(String accountNumber, Long id, String balance) {
        AccountEntity account = new AccountEntity(accountNumber, "Withdraw Owner");
        setField(account, "id", id);
        account.deposit(new BigDecimal(balance));
        return account;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
