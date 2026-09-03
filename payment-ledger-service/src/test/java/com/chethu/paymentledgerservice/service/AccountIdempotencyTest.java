package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.IdempotencyConflictException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

class AccountIdempotencyTest {
    @Test
    void keyedDepositReplaysOriginalBalanceWithoutRepeatingFinancialMutation() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-DEPOSIT", 1L, "500000.00");
        fixture.findByUserIdAnswer(account);
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(wallet(account)));
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(system()));

        AccountResponse first = fixture.service().depositForCurrentUser(42L, money("100000.00"), " K1 ");
        account.deposit(new BigDecimal("50000.00"));
        AccountResponse replay = fixture.service().depositForCurrentUser(42L, money("100000.00"), "K1");

        assertMoney("600000.00", first.getBalance());
        assertMoney("650000.00", account.getBalance());
        assertMoney("600000.00", replay.getBalance());
        verify(fixture.journalRepository).save(any(JournalEntity.class));
        verify(fixture.transactionRepository).save(any(TransactionEntity.class));
        verify(fixture.idempotencyRepository).save(any(IdempotencyRecordEntity.class));
    }

    @Test
    void keyedDepositWithDifferentAmountReturnsConflictAndCreatesNoSecondRecord() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-DEPOSIT", 1L, "500000.00");
        fixture.findByUserIdAnswer(account);
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(wallet(account)));
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(system()));

        fixture.service().depositForCurrentUser(42L, money("100000.00"), "K1");
        assertThrows(IdempotencyConflictException.class,
                () -> fixture.service().depositForCurrentUser(42L, money("200000.00"), "K1"));
        verify(fixture.journalRepository).save(any(JournalEntity.class));
        verify(fixture.transactionRepository).save(any(TransactionEntity.class));
        verify(fixture.idempotencyRepository).save(any(IdempotencyRecordEntity.class));
        assertMoney("600000.00", account.getBalance());
    }

    @Test
    void keyedWithdrawReplaysOriginalBalanceWithoutRepeatingWithdrawal() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-WITHDRAW", 1L, "150000.00");
        fixture.findByUserIdAnswer(account);
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(wallet(account)));
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(system()));

        fixture.service().withdrawForCurrentUser(42L, money("100000.00"), "K2");
        AccountResponse replay = fixture.service().withdrawForCurrentUser(42L, money("100000.00"), "K2");

        assertMoney("50000.00", account.getBalance());
        assertMoney("50000.00", replay.getBalance());
        verify(fixture.journalRepository).save(any(JournalEntity.class));
        verify(fixture.transactionRepository).save(any(TransactionEntity.class));
        verify(fixture.idempotencyRepository).save(any(IdempotencyRecordEntity.class));
    }

    @Test
    void keyedTransferReplaysOnceAndMatchesRecipientInRequestIdentity() {
        Fixture fixture = fixture();
        AccountEntity sender = account("ACC-SENDER", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "50000.00");
        AccountEntity alternateRecipient = account("ACC-OTHER", 3L, "0.00");
        fixture.findByUserIdAnswer(sender);
        when(fixture.accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(fixture.accountRepository.findByAccountNumber("ACC-OTHER")).thenReturn(Optional.of(alternateRecipient));
        when(fixture.ledgerAccountRepository.findByWalletAccount(sender)).thenReturn(Optional.of(wallet(sender)));
        when(fixture.ledgerAccountRepository.findByWalletAccount(recipient)).thenReturn(Optional.of(wallet(recipient)));

        fixture.service().transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), "K3");
        AccountResponse replay = fixture.service().transferForCurrentUser(42L,
                transfer("ACC-RECIPIENT", "100000.00"), "K3");

        assertMoney("100000.00", sender.getBalance());
        assertMoney("150000.00", recipient.getBalance());
        assertMoney("100000.00", replay.getBalance());
        verify(fixture.journalRepository).save(any(JournalEntity.class));
        verify(fixture.transactionRepository, org.mockito.Mockito.times(2)).save(any(TransactionEntity.class));
        verify(fixture.idempotencyRepository).save(any(IdempotencyRecordEntity.class));
        assertThrows(IdempotencyConflictException.class,
                () -> fixture.service().transferForCurrentUser(42L,
                        transfer("ACC-OTHER", "100000.00"), "K3"));
    }

    @Test
    void idempotencyPersistenceFailurePropagatesAfterPostingAttempt() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-FAIL", 1L, "500000.00");
        fixture.findByUserIdAnswer(account);
        when(fixture.ledgerAccountRepository.findByWalletAccount(account)).thenReturn(Optional.of(wallet(account)));
        when(fixture.ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(system()));
        when(fixture.idempotencyRepository.save(any(IdempotencyRecordEntity.class)))
                .thenThrow(new IllegalStateException("idempotency persistence failed"));

        assertThrows(IllegalStateException.class,
                () -> fixture.service().depositForCurrentUser(42L, money("100000.00"), "K4"));
        verify(fixture.idempotencyRepository).save(any(IdempotencyRecordEntity.class));
    }

    private Fixture fixture() {
        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        IdempotencyRecordRepository idempotencyRepository = org.mockito.Mockito.mock(IdempotencyRecordRepository.class);
        List<IdempotencyRecordEntity> records = new ArrayList<>();
        when(idempotencyRepository.findByAccountAndIdempotencyKey(any(), any()))
                .thenAnswer(invocation -> records.stream()
                        .filter(record -> record.getAccount() == invocation.getArgument(0)
                                && record.getIdempotencyKey().equals(invocation.getArgument(1)))
                        .findFirst());
        when(idempotencyRepository.save(any(IdempotencyRecordEntity.class)))
                .thenAnswer(invocation -> {
                    IdempotencyRecordEntity record = invocation.getArgument(0);
                    records.add(record);
                    return record;
                });
        LedgerAccountRepository ledgerAccountRepository = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactionRepository = org.mockito.Mockito.mock(TransactionRepository.class);
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new Fixture(accountRepository, ledgerAccountRepository, journalRepository, transactionRepository,
                idempotencyRepository, records);
    }

    private MoneyOperationRequest money(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private TransferRequest transfer(String recipient, String amount) {
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber(recipient);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private AccountEntity account(String number, Long id, String balance) {
        AccountEntity account = new AccountEntity(number, "Idempotency Owner");
        setField(account, "id", id);
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

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record Fixture(AccountRepository accountRepository, LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository, TransactionRepository transactionRepository,
            IdempotencyRecordRepository idempotencyRepository, List<IdempotencyRecordEntity> records) {
        void findByUserIdAnswer(AccountEntity account) {
            when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        }

        AccountService service() {
            return new AccountService(accountRepository,
                    new TransactionService(transactionRepository, accountRepository),
                    org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgerAccountRepository, journalRepository,
                    new IdempotencyService(idempotencyRepository));
        }
    }
}
