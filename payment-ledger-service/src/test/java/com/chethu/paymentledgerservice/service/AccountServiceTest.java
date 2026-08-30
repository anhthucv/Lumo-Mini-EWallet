package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.CreateAccountRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
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
