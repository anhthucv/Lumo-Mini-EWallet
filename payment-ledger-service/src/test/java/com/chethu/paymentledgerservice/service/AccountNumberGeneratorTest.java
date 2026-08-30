package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.repository.AccountRepository;

class AccountNumberGeneratorTest {

    @Test
    void generateUniqueAccountNumber_shouldReturnFormattedValue() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);

        AccountNumberGenerator generator = new AccountNumberGenerator(accountRepository);

        String accountNumber = generator.generateUniqueAccountNumber();

        assertTrue(accountNumber.matches("ACC-\\d{12}"));
        assertEquals(16, accountNumber.length());
        verify(accountRepository).existsByAccountNumber(accountNumber);
    }

    @Test
    void generateUniqueAccountNumber_shouldRetryOnCollision() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        when(accountRepository.existsByAccountNumber("ACC-000000000001"))
                .thenReturn(true);
        when(accountRepository.existsByAccountNumber("ACC-000000000002"))
                .thenReturn(false);

        Queue<String> candidates = new ArrayBlockingQueue<>(2);
        candidates.add("ACC-000000000001");
        candidates.add("ACC-000000000002");

        AccountNumberGenerator generator = new AccountNumberGenerator(accountRepository) {
            @Override
            protected String generateCandidate() {
                return candidates.remove();
            }
        };

        String accountNumber = generator.generateUniqueAccountNumber();

        assertEquals("ACC-000000000002", accountNumber);
        verify(accountRepository).existsByAccountNumber("ACC-000000000001");
        verify(accountRepository).existsByAccountNumber("ACC-000000000002");
    }
}
