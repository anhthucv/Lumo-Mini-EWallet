package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

import jakarta.persistence.LockModeType;

class ConcurrencyLockingTest {
    @Test
    void accountLockRepositoryMethodUsesPessimisticWrite() throws Exception {
        Lock lock = AccountRepository.class.getMethod("findByIdForUpdate", Long.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    @Test
    void depositMutatesTheLockedAccountInstance() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        AccountEntity unlocked = account("ACC-1", 1L, "500000.00");
        AccountEntity locked = account("ACC-1", 1L, "500000.00");
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(unlocked));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(locked));
        when(accounts.save(locked)).thenReturn(locked);
        AccountService service = service(accounts, locked, null);

        service.depositForCurrentUser(42L, money("100000.00"), null);

        assertMoney("500000.00", unlocked.getBalance());
        assertMoney("600000.00", locked.getBalance());
        verify(accounts).save(locked);
    }

    @Test
    void withdrawUsesLatestLockedBalanceForMinimumRetainedBalance() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        AccountEntity unlocked = account("ACC-1", 1L, "150000.00");
        AccountEntity locked = account("ACC-1", 1L, "50000.00");
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(unlocked));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(locked));
        AccountService service = service(accounts, locked, null);

        assertThrows(RuntimeException.class,
                () -> service.withdrawForCurrentUser(42L, money("100000.00"), null));

        assertMoney("150000.00", unlocked.getBalance());
        assertMoney("50000.00", locked.getBalance());
        verify(accounts, never()).save(any(AccountEntity.class));
    }

    @Test
    void transferLocksLowerIdThenHigherIdEvenWhenDirectionIsReversed() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        AccountEntity sender = account("ACC-B", 20L, "200000.00");
        AccountEntity recipient = account("ACC-A", 10L, "50000.00");
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accounts.findByAccountNumber("ACC-A")).thenReturn(Optional.of(recipient));
        when(accounts.findByIdForUpdate(10L)).thenReturn(Optional.of(recipient));
        when(accounts.findByIdForUpdate(20L)).thenReturn(Optional.of(sender));
        when(accounts.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AccountService service = service(accounts, null, recipient);

        service.transferForCurrentUser(42L, transfer("ACC-A", "100000.00"), null);

        InOrder order = inOrder(accounts);
        order.verify(accounts).findByIdForUpdate(10L);
        order.verify(accounts).findByIdForUpdate(20L);
        assertMoney("100000.00", sender.getBalance());
        assertMoney("150000.00", recipient.getBalance());
    }

    @Test
    void transferLocksTheSameOrderFromLowerIdToHigherIdInForwardDirection() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        AccountEntity sender = account("ACC-A", 10L, "200000.00");
        AccountEntity recipient = account("ACC-B", 20L, "50000.00");
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accounts.findByAccountNumber("ACC-B")).thenReturn(Optional.of(recipient));
        when(accounts.findByIdForUpdate(10L)).thenReturn(Optional.of(sender));
        when(accounts.findByIdForUpdate(20L)).thenReturn(Optional.of(recipient));
        AccountService service = service(accounts, sender, recipient);

        service.transferForCurrentUser(42L, transfer("ACC-B", "100000.00"), null);

        InOrder order = inOrder(accounts);
        order.verify(accounts).findByIdForUpdate(10L);
        order.verify(accounts).findByIdForUpdate(20L);
    }

    @Test
    void lockFailurePropagatesWithoutFinancialPosting() {
        AccountRepository accounts = org.mockito.Mockito.mock(AccountRepository.class);
        AccountEntity account = account("ACC-1", 1L, "500000.00");
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        AccountService service = service(accounts, account, null);

        assertThrows(RuntimeException.class,
                () -> service.depositForCurrentUser(42L, money("100000.00"), null));
        verify(accounts, never()).save(any(AccountEntity.class));
    }

    @Test
    void allMoneyMethodsRemainTransactional() throws Exception {
        assertEquals(Transactional.class,
                AccountService.class.getMethod("depositForCurrentUser", Long.class,
                        MoneyOperationRequest.class, String.class).getAnnotation(Transactional.class).annotationType());
        assertEquals(Transactional.class,
                AccountService.class.getMethod("withdrawForCurrentUser", Long.class,
                        MoneyOperationRequest.class, String.class).getAnnotation(Transactional.class).annotationType());
        assertEquals(Transactional.class,
                AccountService.class.getMethod("transferForCurrentUser", Long.class,
                        TransferRequest.class, String.class).getAnnotation(Transactional.class).annotationType());
    }

    private AccountService service(AccountRepository accounts, AccountEntity lockedAccount,
            AccountEntity recipientAccount) {
        LedgerAccountRepository ledgers = org.mockito.Mockito.mock(LedgerAccountRepository.class);
        JournalRepository journals = org.mockito.Mockito.mock(JournalRepository.class);
        TransactionRepository transactions = org.mockito.Mockito.mock(TransactionRepository.class);
        IdempotencyService idempotency = org.mockito.Mockito.mock(IdempotencyService.class);
        when(idempotency.findExisting(any(), any(), any(), any(), any())).thenReturn(null);
        when(journals.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgers.findByWalletAccount(any(AccountEntity.class))).thenAnswer(invocation ->
                Optional.of(wallet(invocation.getArgument(0))));
        when(ledgers.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(system()));
        when(accounts.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new AccountService(accounts, new TransactionService(transactions, accounts),
                org.mockito.Mockito.mock(AccountNumberGenerator.class), ledgers, journals, idempotency,
                org.mockito.Mockito.mock(TransactionLimitService.class),
                org.mockito.Mockito.mock(RiskEvaluationService.class, invocation -> new RiskEvaluationResult(
                        com.chethu.paymentledgerservice.domain.RiskDecision.ALLOW, java.util.List.of())),
                org.mockito.Mockito.mock(RiskAuditService.class), org.mockito.Mockito.mock(NotificationEventService.class));
    }

    private AccountEntity account(String number, Long id, String balance) {
        AccountEntity account = new AccountEntity(number, "Owner");
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
}
