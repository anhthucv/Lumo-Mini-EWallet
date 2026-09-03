package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.CreateAccountRequest;
import com.chethu.paymentledgerservice.dto.MyWalletResponse;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.RecipientResponse;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.exception.AccountNotActiveException;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.InvalidAccountNumberException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;

class AccountServiceTest {

    @Test
    void createAccount_shouldUseSharedAccountNumberGenerator() {
        AtomicReference<AccountEntity> savedAccount = new AtomicReference<>();
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);

        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity account = invocation.getArgument(0);
            setId(account, 77L);
            savedAccount.set(account);
            return account;
        });

        AccountNumberGenerator generator = new AccountNumberGenerator(accountRepository) {
            @Override
            public String generateUniqueAccountNumber() {
                return "ACC-123456789012";
            }
        };

        AccountService service = service(accountRepository, transactionService, generator);
        CreateAccountRequest request = new CreateAccountRequest();
        request.setOwnerName("Thu");

        AccountResponse response = service.createAccount(request);

        assertEquals(77L, response.getId());
        assertEquals("ACC-123456789012", response.getAccountNumber());
        assertEquals("Thu", response.getOwnerName());
        assertNotNull(savedAccount.get());
        assertEquals("ACC-123456789012", savedAccount.get().getAccountNumber());
        verify(accountRepository).save(any(AccountEntity.class));
    }

    @Test
    void getMyWallet_shouldReturnWalletForAuthenticatedUser() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountNumberGenerator generator = mock(AccountNumberGenerator.class);
        AccountService service = service(accountRepository, transactionService, generator);

        AccountEntity account = new AccountEntity("ACC-999999999999", "Nguyen Van A");
        setId(account, 77L);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));

        MyWalletResponse response = service.getMyWallet(42L);

        assertEquals(77L, response.getAccountId());
        assertEquals("ACC-999999999999", response.getAccountNumber());
        assertEquals("Nguyen Van A", response.getOwnerName());
        assertEquals(BigDecimal.ZERO, response.getBalance());
    }

    @Test
    void getMyWallet_shouldThrowWhenNoWalletExistsForUser() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountNumberGenerator generator = mock(AccountNumberGenerator.class);
        AccountService service = service(accountRepository, transactionService, generator);

        when(accountRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getMyWallet(42L));
        verify(accountRepository).findByUserId(42L);
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void getRecipient_shouldFindByAccountNumberAndReturnSafeDetails() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));
        AccountEntity account = new AccountEntity("ACC-123456789012", "Nguyen Van B");
        when(accountRepository.findByAccountNumber("ACC-123456789012")).thenReturn(Optional.of(account));

        RecipientResponse response = service.getRecipient("ACC-123456789012");

        assertEquals("ACC-123456789012", response.getAccountNumber());
        assertEquals("Nguyen Van B", response.getOwnerName());
        verify(accountRepository).findByAccountNumber("ACC-123456789012");
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void getRecipient_shouldThrowWhenAccountNumberDoesNotExist() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));
        when(accountRepository.findByAccountNumber("ACC-MISSING")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getRecipient("ACC-MISSING"));
        verify(accountRepository).findByAccountNumber("ACC-MISSING");
    }

    @Test
    void getRecipient_shouldRejectBlankAccountNumber() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));

        assertThrows(InvalidAccountNumberException.class, () -> service.getRecipient("  "));
        verify(accountRepository, never()).findByAccountNumber(any());
    }

    @Test
    void transferForCurrentUser_shouldMoveFundsAndRecordTwoTransferTransactions() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "200000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", "Recipient", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = service.transferForCurrentUser(42L,
                transferRequest("ACC-RECIPIENT", "100000.00"), null);

        assertEquals(new BigDecimal("100000.00"), response.getBalance());
        assertEquals(new BigDecimal("100000.00"), sender.getBalance());
        assertEquals(new BigDecimal("100000.00"), recipient.getBalance());
        verify(accountRepository).findByUserId(42L);
        verify(accountRepository).findByAccountNumber("ACC-RECIPIENT");
        verify(accountRepository).save(sender);
        verify(accountRepository).save(recipient);
        verify(transactionService).recordTransaction(eq(sender), eq(recipient), eq(TransactionType.TRANSFER_OUT),
                eq(new BigDecimal("100000.00")), eq(new BigDecimal("100000.00")), any(JournalEntity.class));
        verify(transactionService).recordTransaction(eq(recipient), eq(sender), eq(TransactionType.TRANSFER_IN),
                eq(new BigDecimal("100000.00")), eq(new BigDecimal("100000.00")), any(JournalEntity.class));
    }

    @Test
    void transferForCurrentUser_shouldAllowExactlyMinimumRemainingBalance() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));
        AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "100000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", "Recipient", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.transferForCurrentUser(42L, transferRequest("ACC-RECIPIENT", "50000.00"), null);

        assertEquals(new BigDecimal("50000.00"), sender.getBalance());
    }

    @Test
    void transferForCurrentUser_shouldRejectBelowMinimumRemainingBalance() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "100000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", "Recipient", 2L, "0.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));

        assertThrows(com.chethu.paymentledgerservice.exception.InsufficientBalanceException.class,
                () -> service.transferForCurrentUser(42L, transferRequest("ACC-RECIPIENT", "60000.00"), null));
        assertEquals(new BigDecimal("100000.00"), sender.getBalance());
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void transferForCurrentUser_shouldRejectSelfTransfer() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));
        AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "200000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-SENDER")).thenReturn(Optional.of(sender));

        assertThrows(com.chethu.paymentledgerservice.exception.InvalidTransferException.class,
                () -> service.transferForCurrentUser(42L, transferRequest("ACC-SENDER", "100000.00"), null));
    }

    @Test
    void transferForCurrentUser_shouldRejectAmountBelowMinimum() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));

        assertThrows(com.chethu.paymentledgerservice.exception.InvalidTransferException.class,
                () -> service.transferForCurrentUser(42L, transferRequest("ACC-RECIPIENT", "999.99"), null));
        verify(accountRepository, never()).findByUserId(any());
    }

    @Test
    void transferForCurrentUser_shouldThrowWhenRecipientDoesNotExist() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        AccountService service = service(accountRepository, mock(TransactionService.class),
                mock(AccountNumberGenerator.class));
        AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "200000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumber("ACC-MISSING")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.transferForCurrentUser(42L, transferRequest("ACC-MISSING", "100000.00"), null));
    }

    @Test
    void depositForCurrentUser_shouldUpdateOwnWalletAndRecordDeposit() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountNumberGenerator generator = mock(AccountNumberGenerator.class);
        LedgerAccountRepository ledgerAccountRepository = mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = mock(JournalRepository.class);
        when(ledgerAccountRepository.findByWalletAccount(any())).thenReturn(Optional.empty());
        when(ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.empty());
        when(ledgerAccountRepository.save(any(LedgerAccountEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(journalRepository.save(any(JournalEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AccountService service = new AccountService(accountRepository, transactionService, generator,
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));

        AccountEntity account = new AccountEntity("ACC-999999999999", "Nguyen Van A");
        setId(account, 77L);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        MoneyOperationRequest request = moneyRequest("100000.00");

        AccountResponse response = service.depositForCurrentUser(42L, request, null);

        assertEquals(new BigDecimal("100000.00"), response.getBalance());
        verify(accountRepository).findByUserId(42L);
        verify(accountRepository).save(account);
        verify(transactionService).recordTransaction(
                eq(account), isNull(), eq(TransactionType.DEPOSIT),
                eq(new BigDecimal("100000.00")), eq(new BigDecimal("100000.00")), any(JournalEntity.class));
    }

    @Test
    void depositForCurrentUser_shouldRejectAmountBelowMinimum() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService, mock(AccountNumberGenerator.class));
        AccountEntity account = new AccountEntity("ACC-999999999999", "Nguyen Van A");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class,
                () -> service.depositForCurrentUser(42L, moneyRequest("999.99"), null));
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void depositForCurrentUser_shouldRejectFrozenWalletBeforeMutation() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity account = account("ACC-FROZEN", "Nguyen Van A", 77L, "100000.00");
        setStatus(account, AccountStatus.FROZEN);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));

        assertThrows(AccountNotActiveException.class,
                () -> service.depositForCurrentUser(42L, moneyRequest("100000.00"), null));
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void depositForCurrentUser_shouldRejectClosedWalletBeforeMutation() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity account = account("ACC-CLOSED", "Nguyen Van A", 77L, "100000.00");
        setStatus(account, AccountStatus.CLOSED);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));

        assertThrows(AccountNotActiveException.class,
                () -> service.depositForCurrentUser(42L, moneyRequest("100000.00"), null));
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void withdrawForCurrentUser_shouldAllowActiveWalletAndRecordLedger() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity account = account("ACC-ACTIVE", "Nguyen Van A", 77L, "200000.00");
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        AccountResponse response = service.withdrawForCurrentUser(42L, moneyRequest("100000.00"), null);

        assertEquals(new BigDecimal("100000.00"), response.getBalance());
        verify(transactionService).recordTransaction(eq(account), isNull(), eq(TransactionType.WITHDRAW),
                eq(new BigDecimal("100000.00")), eq(new BigDecimal("100000.00")), any(JournalEntity.class));
    }

    @Test
    void withdrawForCurrentUser_shouldRejectFrozenWalletWithoutMutation() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity account = account("ACC-FROZEN", "Nguyen Van A", 77L, "200000.00");
        setStatus(account, AccountStatus.FROZEN);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));

        assertThrows(AccountNotActiveException.class,
                () -> service.withdrawForCurrentUser(42L, moneyRequest("100000.00"), null));
        assertEquals(new BigDecimal("200000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void withdrawForCurrentUser_shouldRejectClosedWalletWithoutMutation() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService,
                mock(AccountNumberGenerator.class));
        AccountEntity account = account("ACC-CLOSED", "Nguyen Van A", 77L, "200000.00");
        setStatus(account, AccountStatus.CLOSED);
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(account));

        assertThrows(AccountNotActiveException.class,
                () -> service.withdrawForCurrentUser(42L, moneyRequest("100000.00"), null));
        assertEquals(new BigDecimal("200000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    @Test
    void transferForCurrentUser_shouldRejectInactiveSenderAndRecipientWithoutMutation() {
        for (AccountStatus inactiveStatus : new AccountStatus[] { AccountStatus.FROZEN, AccountStatus.CLOSED }) {
            AccountRepository accountRepository = mock(AccountRepository.class);
            TransactionService transactionService = mock(TransactionService.class);
            AccountService service = service(accountRepository, transactionService,
                    mock(AccountNumberGenerator.class));
            AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "200000.00");
            AccountEntity recipient = account("ACC-RECIPIENT", "Recipient", 2L, "0.00");
            setStatus(sender, inactiveStatus);
            when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
            when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
            when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));

            assertThrows(AccountNotActiveException.class,
                    () -> service.transferForCurrentUser(42L, transferRequest("ACC-RECIPIENT", "100000.00"), null));
            assertEquals(new BigDecimal("200000.00"), sender.getBalance());
            assertEquals(new BigDecimal("0.00"), recipient.getBalance());
            verify(accountRepository, never()).save(any(AccountEntity.class));
            verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
        }
    }

    @Test
    void transferForCurrentUser_shouldRejectInactiveRecipientWithoutMutation() {
        for (AccountStatus inactiveStatus : new AccountStatus[] { AccountStatus.FROZEN, AccountStatus.CLOSED }) {
            AccountRepository accountRepository = mock(AccountRepository.class);
            TransactionService transactionService = mock(TransactionService.class);
            AccountService service = service(accountRepository, transactionService,
                    mock(AccountNumberGenerator.class));
            AccountEntity sender = account("ACC-SENDER", "Sender", 1L, "200000.00");
            AccountEntity recipient = account("ACC-RECIPIENT", "Recipient", 2L, "0.00");
            setStatus(recipient, inactiveStatus);
            when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(sender));
            when(accountRepository.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
            when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));

            assertThrows(AccountNotActiveException.class,
                    () -> service.transferForCurrentUser(42L, transferRequest("ACC-RECIPIENT", "100000.00"), null));
            assertEquals(new BigDecimal("200000.00"), sender.getBalance());
            assertEquals(new BigDecimal("0.00"), recipient.getBalance());
            verify(accountRepository, never()).save(any(AccountEntity.class));
            verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
        }
    }

    @Test
    void depositForCurrentUser_shouldThrowWhenWalletDoesNotExist() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionService transactionService = mock(TransactionService.class);
        AccountService service = service(accountRepository, transactionService, mock(AccountNumberGenerator.class));
        when(accountRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.depositForCurrentUser(42L, moneyRequest("100000"), null));
        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionService, never()).recordTransaction(any(), any(), any(), any(), any());
    }

    private MoneyOperationRequest moneyRequest(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private AccountService service(AccountRepository accountRepository, TransactionService transactionService,
            AccountNumberGenerator accountNumberGenerator) {
        LedgerAccountRepository ledgerAccountRepository = mock(LedgerAccountRepository.class);
        JournalRepository journalRepository = mock(JournalRepository.class);
        when(ledgerAccountRepository.findByWalletAccount(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity account = invocation.getArgument(0);
            return Optional.of(new LedgerAccountEntity("WALLET-" + account.getAccountNumber(),
                    LedgerAccountType.WALLET, AccountClass.LIABILITY, account));
        });
        when(ledgerAccountRepository.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(
                new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                        AccountClass.ASSET, null)));
        when(journalRepository.save(any(JournalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new AccountService(accountRepository, transactionService, accountNumberGenerator,
                ledgerAccountRepository, journalRepository,
                new IdempotencyService(org.mockito.Mockito.mock(
                        com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository.class)));
    }

    private TransferRequest transferRequest(String recipientAccountNumber, String amount) {
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber(recipientAccountNumber);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private AccountEntity account(String accountNumber, String ownerName, Long id, String balance) {
        AccountEntity account = new AccountEntity(accountNumber, ownerName);
        setId(account, id);
        account.deposit(new BigDecimal(balance));
        return account;
    }

    private void setId(AccountEntity account, Long id) {
        try {
            Field field = AccountEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set account id", ex);
        }
    }

    private void setStatus(AccountEntity account, AccountStatus status) {
        try {
            Field field = AccountEntity.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(account, status);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set account status", ex);
        }
    }
}
