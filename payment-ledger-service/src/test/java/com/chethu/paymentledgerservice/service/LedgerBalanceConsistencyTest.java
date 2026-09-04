package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
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

class LedgerBalanceConsistencyTest {
    @Test
    void journalBalanceUsesNumericBigDecimalEqualityAndRejectsEmptyOrUnequalJournals() {
        LedgerAccountEntity debitAccount = systemAccount();
        LedgerAccountEntity creditAccount = walletAccount(account("ACC-1", 1L, "0.00"));

        JournalEntity balanced = new JournalEntity("BALANCED");
        new LedgerEntryEntity(balanced, debitAccount, LedgerEntryType.DEBIT, new BigDecimal("100.0"));
        new LedgerEntryEntity(balanced, creditAccount, LedgerEntryType.CREDIT, new BigDecimal("100.00"));
        assertTrue(balanced.isBalanced());

        JournalEntity unequal = new JournalEntity("UNEQUAL");
        new LedgerEntryEntity(unequal, debitAccount, LedgerEntryType.DEBIT, new BigDecimal("100.01"));
        new LedgerEntryEntity(unequal, creditAccount, LedgerEntryType.CREDIT, new BigDecimal("100.00"));
        assertFalse(unequal.isBalanced());
        assertFalse(new JournalEntity("EMPTY").isBalanced());
    }

    @Test
    void depositKeepsBalanceAndJournalTransactionConsistent() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-DEPOSIT", 1L, "500000.00");
        when(fixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(fixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.empty());
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.empty());
        when(fixture.ledgerAccountRepository.save(any(LedgerAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = fixture.service().depositForCurrentUser(42L, money("100000.00"), null);
        JournalEntity journal = captureJournal(fixture.journalRepository);
        TransactionEntity transaction = captureTransaction(fixture.transactionRepository);

        assertLedgerTotals(journal, "100000.00");
        assertEquals(LedgerEntryType.DEBIT, journal.getEntries().get(0).getEntryType());
        assertEquals(LedgerAccountType.SYSTEM_CLEARING, journal.getEntries().get(0).getLedgerAccount().getType());
        assertEquals(AccountClass.ASSET, journal.getEntries().get(0).getLedgerAccount().getAccountClass());
        assertEquals(LedgerEntryType.CREDIT, journal.getEntries().get(1).getEntryType());
        assertEquals(LedgerAccountType.WALLET, journal.getEntries().get(1).getLedgerAccount().getType());
        assertEquals(AccountClass.LIABILITY, journal.getEntries().get(1).getLedgerAccount().getAccountClass());
        assertMoney("600000.00", account.getBalance());
        assertMoney("600000.00", response.getBalance());
        assertMoney("600000.00", transaction.getBalanceAfterTransaction());
        assertEquals(TransactionType.DEPOSIT, transaction.getTransactionType());
        assertEquals(journal, transaction.getJournal());
    }

    @Test
    void withdrawKeepsBalanceAndJournalTransactionConsistentAtMinimumBalance() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-WITHDRAW", 1L, "150000.00");
        LedgerAccountEntity wallet = walletAccount(account);
        LedgerAccountEntity clearing = systemAccount();
        when(fixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(fixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(wallet));
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(clearing));

        fixture.service().withdrawForCurrentUser(42L, money("100000.00"), null);
        JournalEntity journal = captureJournal(fixture.journalRepository);
        TransactionEntity transaction = captureTransaction(fixture.transactionRepository);

        assertLedgerTotals(journal, "100000.00");
        assertEquals(LedgerEntryType.DEBIT, journal.getEntries().get(0).getEntryType());
        assertEquals(LedgerAccountType.WALLET, journal.getEntries().get(0).getLedgerAccount().getType());
        assertEquals(LedgerEntryType.CREDIT, journal.getEntries().get(1).getEntryType());
        assertEquals(LedgerAccountType.SYSTEM_CLEARING, journal.getEntries().get(1).getLedgerAccount().getType());
        assertMoney("50000.00", account.getBalance());
        assertMoney("50000.00", transaction.getBalanceAfterTransaction());
        assertEquals(TransactionType.WITHDRAW, transaction.getTransactionType());
        assertEquals(journal, transaction.getJournal());

        assertThrows(RuntimeException.class, () -> fixture.service().withdrawForCurrentUser(42L, money("1.00"), null));
        verify(fixture.journalRepository).save(journal);
    }

    @Test
    void transferBalancesWalletsAndConservesValueWithOneSharedJournal() {
        Fixture fixture = fixture();
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "50000.00");
        when(fixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(fixture.accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(fixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(fixture.accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(fixture.ledgerAccountRepository.findByWalletAccount(sender)).thenReturn(Optional.of(walletAccount(sender)));
        when(fixture.ledgerAccountRepository.findByWalletAccount(recipient)).thenReturn(Optional.of(walletAccount(recipient)));

        BigDecimal oldTotal = sender.getBalance().add(recipient.getBalance());
        fixture.service().transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null);
        JournalEntity journal = captureJournal(fixture.journalRepository);
        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(fixture.transactionRepository, org.mockito.Mockito.times(2)).save(transactionCaptor.capture());

        assertLedgerTotals(journal, "100000.00");
        assertEquals(LedgerEntryType.DEBIT, journal.getEntries().get(0).getEntryType());
        assertEquals(sender, journal.getEntries().get(0).getLedgerAccount().getWalletAccount());
        assertEquals(LedgerEntryType.CREDIT, journal.getEntries().get(1).getEntryType());
        assertEquals(recipient, journal.getEntries().get(1).getLedgerAccount().getWalletAccount());
        verify(fixture.ledgerAccountRepository, never()).findByCode("SYSTEM_CLEARING");
        assertMoney("100000.00", sender.getBalance());
        assertMoney("150000.00", recipient.getBalance());
        assertMoney(oldTotal.toPlainString(), sender.getBalance().add(recipient.getBalance()));
        assertEquals(TransactionType.TRANSFER_OUT, transactionCaptor.getAllValues().get(0).getTransactionType());
        assertEquals(TransactionType.TRANSFER_IN, transactionCaptor.getAllValues().get(1).getTransactionType());
        assertMoney("100000.00", transactionCaptor.getAllValues().get(0).getBalanceAfterTransaction());
        assertMoney("150000.00", transactionCaptor.getAllValues().get(1).getBalanceAfterTransaction());
        assertEquals(journal, transactionCaptor.getAllValues().get(0).getJournal());
        assertEquals(journal, transactionCaptor.getAllValues().get(1).getJournal());
    }

    @Test
    void rejectedOperationsDoNotCreatePartialFinancialRecords() {
        Fixture frozenFixture = fixture();
        AccountEntity frozen = account("ACC-FROZEN", 1L, "100000.00");
        setField(frozen, "status", AccountStatus.FROZEN);
        when(frozenFixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(frozen));
        when(frozenFixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(frozen));
        assertThrows(AccountNotActiveException.class,
                () -> frozenFixture.service().depositForCurrentUser(42L, money("100000.00"), null));
        assertMoney("100000.00", frozen.getBalance());
        verify(frozenFixture.journalRepository, never()).save(any(JournalEntity.class));
        verify(frozenFixture.transactionRepository, never()).save(any(TransactionEntity.class));

        Fixture invalidTransferFixture = fixture();
        AccountEntity sender = account("ACC-SENDER", 1L, "100000.00");
        when(invalidTransferFixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(invalidTransferFixture.accountRepository.findByAccountNumber("ACC-SENDER"))
                .thenReturn(Optional.of(sender));
        when(invalidTransferFixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        assertThrows(InvalidTransferException.class,
                () -> invalidTransferFixture.service().transferForCurrentUser(42L, transfer("ACC-SENDER", "1.00"), null));
        verify(invalidTransferFixture.journalRepository, never()).save(any(JournalEntity.class));
        verify(invalidTransferFixture.transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void invalidDepositAndWithdrawAmountsDoNotCreateFinancialRecords() {
        for (boolean deposit : new boolean[] { true, false }) {
            Fixture fixture = fixture();
            AccountEntity account = account("ACC-INVALID-" + deposit, 1L, "100000.00");
            when(fixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
            when(fixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

            assertThrows(IllegalArgumentException.class, () -> {
                if (deposit) {
                    fixture.service().depositForCurrentUser(42L, money("0.99"), null);
                } else {
                    fixture.service().withdrawForCurrentUser(42L, money("0.99"), null);
                }
            });

            assertMoney("100000.00", account.getBalance());
            verify(fixture.journalRepository, never()).save(any(JournalEntity.class));
            verify(fixture.transactionRepository, never()).save(any(TransactionEntity.class));
        }
    }

    @Test
    void repeatedDepositReusesWalletAndSystemLedgerAccounts() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-REUSE", 1L, "500000.00");
        when(fixture.accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(fixture.accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(walletAccount(account)));
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(systemAccount()));

        fixture.service().depositForCurrentUser(42L, money("100000.00"), null);
        fixture.service().depositForCurrentUser(42L, money("50000.00"), null);

        verify(fixture.ledgerAccountRepository, never()).save(any(LedgerAccountEntity.class));
        assertMoney("650000.00", account.getBalance());
        verify(fixture.journalRepository, org.mockito.Mockito.times(2)).save(any(JournalEntity.class));
    }

    private void assertLedgerTotals(JournalEntity journal, String amount) {
        assertEquals(2, journal.getEntries().size());
        assertTrue(journal.isBalanced());
        assertMoney(amount, sum(journal, LedgerEntryType.DEBIT));
        assertMoney(amount, sum(journal, LedgerEntryType.CREDIT));
        for (LedgerEntryEntity entry : journal.getEntries()) {
            assertTrue(entry.getAmount().signum() > 0);
            assertEquals(journal, entry.getJournal());
            assertTrue(entry.getLedgerAccount() != null);
            assertTrue(entry.getEntryType() != null);
        }
    }

    private BigDecimal sum(JournalEntity journal, LedgerEntryType type) {
        return journal.getEntries().stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(LedgerEntryEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private JournalEntity captureJournal(JournalRepository repository) {
        ArgumentCaptor<JournalEntity> captor = ArgumentCaptor.forClass(JournalEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private TransactionEntity captureTransaction(TransactionRepository repository) {
        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private Fixture fixture() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new Fixture(accountRepository, ledgerAccountRepository, journalRepository, transactionRepository);
    }

    private MoneyOperationRequest money(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private TransferRequest transfer(String accountNumber, String amount) {
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber(accountNumber);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private AccountEntity account(String accountNumber, Long id, String balance) {
        AccountEntity account = new AccountEntity(accountNumber, "Ledger Test Owner");
        setField(account, "id", id);
        account.deposit(new BigDecimal(balance));
        return account;
    }

    private LedgerAccountEntity walletAccount(AccountEntity account) {
        return new LedgerAccountEntity("WALLET-" + account.getAccountNumber(), LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account);
    }

    private LedgerAccountEntity systemAccount() {
        return new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                AccountClass.ASSET, null);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
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

    private record Fixture(AccountRepository accountRepository, LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository, TransactionRepository transactionRepository) {
        AccountService service() {
            TransactionService transactionService = new TransactionService(transactionRepository, accountRepository);
            return new AccountService(accountRepository, transactionService,
                    org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository,
                    new IdempotencyService(org.mockito.Mockito.mock(
                            com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)),
                    org.mockito.Mockito.mock(TransactionLimitService.class),
                    org.mockito.Mockito.mock(RiskEvaluationService.class, invocation -> new RiskEvaluationResult(
                            com.chethu.paymentledgerservice.domain.RiskDecision.ALLOW, java.util.List.of())),
                    org.mockito.Mockito.mock(RiskAuditService.class), org.mockito.Mockito.mock(NotificationEventService.class));
        }
    }
}
