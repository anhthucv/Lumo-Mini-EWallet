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
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotActiveException;
import com.chethu.paymentledgerservice.exception.InvalidTransferException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

class TransferLedgerPostingTest {
    @Test
    void transfer_shouldCreateOneBalancedJournalAndTwoLinkedSuccessTransactions() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "50000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerAccountRepository.findByWalletAccount(sender)).thenReturn(Optional.of(walletLedger(sender)));
        when(ledgerAccountRepository.findByWalletAccount(recipient)).thenReturn(Optional.of(walletLedger(recipient)));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionService transactionService = new TransactionService(transactionRepository, accountRepository);
        AccountService service = new AccountService(accountRepository, transactionService,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        AccountResponse response = service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null);

        assertEquals(new BigDecimal("100000.00"), response.getBalance());
        assertEquals(new BigDecimal("100000.00"), sender.getBalance());
        assertEquals(new BigDecimal("150000.00"), recipient.getBalance());

        ArgumentCaptor<JournalEntity> journalCaptor = ArgumentCaptor.forClass(JournalEntity.class);
        verify(journalRepository).save(journalCaptor.capture());
        JournalEntity journal = journalCaptor.getValue();
        assertEquals(2, journal.getEntries().size());
        LedgerEntryEntity senderEntry = journal.getEntries().get(0);
        LedgerEntryEntity recipientEntry = journal.getEntries().get(1);
        assertEquals(LedgerEntryType.DEBIT, senderEntry.getEntryType());
        assertEquals(LedgerEntryType.CREDIT, recipientEntry.getEntryType());
        assertEquals(new BigDecimal("100000.00"), senderEntry.getAmount());
        assertEquals(new BigDecimal("100000.00"), recipientEntry.getAmount());
        assertEquals(LedgerAccountType.WALLET, senderEntry.getLedgerAccount().getType());
        assertEquals(AccountClass.LIABILITY, senderEntry.getLedgerAccount().getAccountClass());
        assertEquals(LedgerAccountType.WALLET, recipientEntry.getLedgerAccount().getType());
        assertEquals(AccountClass.LIABILITY, recipientEntry.getLedgerAccount().getAccountClass());
        assertEquals(true, journal.isBalanced());

        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(transactionCaptor.capture());
        TransactionEntity outgoing = transactionCaptor.getAllValues().get(0);
        TransactionEntity incoming = transactionCaptor.getAllValues().get(1);
        assertEquals(TransactionType.TRANSFER_OUT, outgoing.getTransactionType());
        assertEquals(TransactionType.TRANSFER_IN, incoming.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, outgoing.getStatus());
        assertEquals(TransactionStatus.SUCCESS, incoming.getStatus());
        assertEquals(new BigDecimal("100000.00"), outgoing.getBalanceAfterTransaction());
        assertEquals(new BigDecimal("150000.00"), incoming.getBalanceAfterTransaction());
        assertEquals(journal, outgoing.getJournal());
        assertEquals(journal, incoming.getJournal());
        verify(ledgerAccountRepository, never()).findByCode("SYSTEM_CLEARING");
    }

    @Test
    void transfer_shouldReuseExistingWalletLedgerAccounts() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerAccountRepository.findByWalletAccount(sender)).thenReturn(Optional.of(walletLedger(sender)));
        when(ledgerAccountRepository.findByWalletAccount(recipient)).thenReturn(Optional.of(walletLedger(recipient)));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null);

        verify(ledgerAccountRepository, never()).save(any(LedgerAccountEntity.class));
    }

    @Test
    void transfer_rejectedRulesShouldNotMutateOrPost() {
        for (AccountStatus status : new AccountStatus[] { AccountStatus.FROZEN, AccountStatus.CLOSED }) {
            AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
            LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
            JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
            AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
            AccountEntity recipient = account("ACC-RECIPIENT", 2L, "0.00");
            setField(sender, "status", status);
            when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
            when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
            when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
            AccountService service = new AccountService(accountRepository,
                    org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                    ledgerAccountRepository, journalRepository,
                    new IdempotencyService(org.mockito.Mockito.mock(
                            com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

            assertThrows(AccountNotActiveException.class,
                    () -> service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null));
            assertEquals(new BigDecimal("200000.00"), sender.getBalance());
            assertEquals(new BigDecimal("0.00"), recipient.getBalance());
            verify(journalRepository, never()).save(any(JournalEntity.class));
        }
    }

    @Test
    void transfer_selfTransferShouldNotMutateOrPost() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-SENDER")).thenReturn(Optional.of(sender));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        assertThrows(InvalidTransferException.class,
                () -> service.transferForCurrentUser(42L, transfer("ACC-SENDER", "100000.00"), null));
        assertEquals(new BigDecimal("200000.00"), sender.getBalance());
        verify(journalRepository, never()).save(any(JournalEntity.class));
    }

    @Test
    void transfer_insufficientFundsShouldNotPost() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity sender = account("ACC-SENDER", 1L, "100000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        assertThrows(RuntimeException.class,
                () -> service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "60000.00"), null));
        assertEquals(new BigDecimal("100000.00"), sender.getBalance());
        assertEquals(new BigDecimal("0.00"), recipient.getBalance());
        verify(journalRepository, never()).save(any(JournalEntity.class));
    }

    @Test
    void transfer_ledgerPersistenceFailureShouldPropagate() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(ledgerAccountRepository.findByWalletAccount(sender)).thenReturn(Optional.of(walletLedger(sender)));
        when(ledgerAccountRepository.findByWalletAccount(recipient)).thenReturn(Optional.of(walletLedger(recipient)));
        when(journalRepository.save(any(JournalEntity.class)))
                .thenThrow(new IllegalStateException("ledger persistence failed"));
        AccountService service = new AccountService(accountRepository,
                org.mockito.Mockito.mock(TransactionService.class), org.mockito.Mockito.mock(AccountNumberGenerator.class),
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        assertThrows(IllegalStateException.class,
                () -> service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null));
    }

    @Test
    void transfer_transactionPersistenceFailureShouldPropagate() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerAccountRepository.findByWalletAccount(sender)).thenReturn(Optional.of(walletLedger(sender)));
        when(ledgerAccountRepository.findByWalletAccount(recipient)).thenReturn(Optional.of(walletLedger(recipient)));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenThrow(new IllegalStateException("transaction persistence failed"));
        TransactionService transactionService = new TransactionService(transactionRepository, accountRepository);
        AccountService service = new AccountService(accountRepository, transactionService,
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        assertThrows(IllegalStateException.class,
                () -> service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null));
    }

    @Test
    void transferForCurrentUser_shouldRemainTransactional() throws Exception {
        assertEquals(Transactional.class,
                AccountService.class.getMethod("transferForCurrentUser", Long.class, TransferRequest.class,
                        String.class)
                        .getAnnotation(Transactional.class).annotationType());
    }

    private TransferRequest transfer(String accountNumber, String amount) {
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber(accountNumber);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private LedgerAccountEntity walletLedger(AccountEntity account) {
        return new LedgerAccountEntity("WALLET-" + account.getAccountNumber(), LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account);
    }

    private AccountEntity account(String accountNumber, Long id, String balance) {
        AccountEntity account = new AccountEntity(accountNumber, "Transfer Owner");
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
