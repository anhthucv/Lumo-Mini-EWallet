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

class DepositLedgerPostingTest {
    @Test
    void deposit_shouldCreateBalancedJournalAndLinkedSuccessTransaction() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        AccountEntity account = account("ACC-100", 42L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.empty());
        when(ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.empty());
        when(ledgerAccountRepository.save(any(LedgerAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(journalRepository.save(any(JournalEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionService transactionService = new TransactionService(transactionRepository, accountRepository);
        AccountService service = new AccountService(accountRepository, transactionService,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository);

        AccountResponse response = service.depositForCurrentUser(42L, money("100000.00"));

        assertEquals(new BigDecimal("100000.00"), response.getBalance());
        ArgumentCaptor<JournalEntity> journalCaptor = ArgumentCaptor.forClass(JournalEntity.class);
        verify(journalRepository).save(journalCaptor.capture());
        JournalEntity journal = journalCaptor.getValue();
        assertEquals(2, journal.getEntries().size());
        assertEquals(LedgerEntryType.DEBIT, journal.getEntries().get(0).getEntryType());
        assertEquals(LedgerEntryType.CREDIT, journal.getEntries().get(1).getEntryType());
        assertEquals(new BigDecimal("100000.00"), journal.getEntries().get(0).getAmount());
        assertEquals(new BigDecimal("100000.00"), journal.getEntries().get(1).getAmount());
        assertEquals(AccountClass.ASSET, journal.getEntries().get(0).getLedgerAccount().getAccountClass());
        assertEquals(LedgerAccountType.SYSTEM_CLEARING, journal.getEntries().get(0).getLedgerAccount().getType());
        assertEquals(AccountClass.LIABILITY, journal.getEntries().get(1).getLedgerAccount().getAccountClass());
        assertEquals(LedgerAccountType.WALLET, journal.getEntries().get(1).getLedgerAccount().getType());
        assertTrueBalanced(journal);

        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        TransactionEntity transaction = transactionCaptor.getValue();
        assertEquals(TransactionType.DEPOSIT, transaction.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
        assertEquals(new BigDecimal("100000.00"), transaction.getBalanceAfterTransaction());
        assertEquals(journal, transaction.getJournal());
    }

    @Test
    void deposit_shouldReuseExistingWalletAndSystemLedgerAccounts() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        AccountEntity account = account("ACC-100", 42L, "0.00");
        LedgerAccountEntity walletLedger = new LedgerAccountEntity("WALLET-ACC-100", LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account);
        LedgerAccountEntity clearing = new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                AccountClass.ASSET, null);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(walletLedger));
        when(ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(clearing));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionService transactionService = new TransactionService(transactionRepository, accountRepository);
        AccountService service = new AccountService(accountRepository, transactionService,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository);

        service.depositForCurrentUser(42L, money("100000.00"));
        service.depositForCurrentUser(42L, money("50000.00"));

        verify(ledgerAccountRepository, never()).save(any(LedgerAccountEntity.class));
        assertEquals(new BigDecimal("150000.00"), account.getBalance());
    }

    @Test
    void rejectedDeposit_shouldNotResolveOrCreateLedgerPosting() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity account = account("ACC-100", 42L, "100000.00");
        setStatus(account, AccountStatus.FROZEN);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository);

        assertThrows(AccountNotActiveException.class,
                () -> service.depositForCurrentUser(42L, money("100000.00")));
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(ledgerAccountRepository, never()).findByWalletAccount(any());
        verify(journalRepository, never()).save(any(JournalEntity.class));
    }

    private MoneyOperationRequest money(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private AccountEntity account(String accountNumber, Long id, String balance) {
        AccountEntity account = new AccountEntity(accountNumber, "Deposit Owner");
        setField(account, "id", id);
        account.deposit(new BigDecimal(balance));
        return account;
    }

    private void setStatus(AccountEntity account, AccountStatus status) {
        setField(account, "status", status);
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

    private void assertTrueBalanced(JournalEntity journal) {
        if (!journal.isBalanced()) {
            throw new AssertionError("Deposit journal must be balanced");
        }
    }
}
