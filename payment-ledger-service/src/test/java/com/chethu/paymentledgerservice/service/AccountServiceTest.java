package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;

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

        AccountService service = new AccountService(accountRepository, transactionService, generator);
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
        AccountService service = new AccountService(accountRepository, transactionService, generator);

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
        AccountService service = new AccountService(accountRepository, transactionService, generator);

        when(accountRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getMyWallet(42L));
        verify(accountRepository).findByUserId(42L);
        verify(accountRepository, never()).save(any(AccountEntity.class));
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
}
