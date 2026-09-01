package com.chethu.paymentledgerservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.TransactionNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getHistoryForUser_shouldThrowException_whenWalletNotFound(){
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0,5);
        when (accountRepository.findByUserId(userId)).thenReturn(java.util.Optional.empty());
        
        assertThrows(AccountNotFoundException.class, () -> transactionService.getHistoryForUser(
                userId, null, null, null, null, null, pageable));
    }

    @Test 
    void getHistoryForUser_shouldReturnHistoryForResolvedWallet(){
        Long userId = 42L;
        Pageable pageable = PageRequest.of(0,5);

        AccountEntity account = new AccountEntity("ACC-Test","Test user");
        when(accountRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(account));
        TransactionEntity transaction = new TransactionEntity(account,null, TransactionType.DEPOSIT,new BigDecimal("50000.00"),new BigDecimal("50000.00"));

        Page<TransactionEntity> transactionPage = new PageImpl<>(List.of(transaction));
        when (transactionRepository.findAll(org.mockito.ArgumentMatchers.<Specification<TransactionEntity>>any(),
                org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(transactionPage);

        Page<TransactionResponse> result = transactionService.getHistoryForUser(
                userId, TransactionType.DEPOSIT, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30), new BigDecimal("100.00"), new BigDecimal("100000.00"), pageable);

        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
        assertNull( response.getRelatedAccountId());
        assertEquals(new BigDecimal("50000.00"), response.getAmount());   
        assertEquals(new BigDecimal("50000.00"), response.getBalanceAfterTransaction());           
        verify(accountRepository).findByUserId(userId);
        verify(transactionRepository).findAll(org.mockito.ArgumentMatchers.<Specification<TransactionEntity>>any(),
                org.mockito.ArgumentMatchers.eq(pageable));
    } 

    @Test
    void getHistoryForUser_shouldRejectInvalidFilterRangesBeforeQuerying() {
        assertThrows(com.chethu.paymentledgerservice.exception.InvalidTransactionFilterException.class,
                () -> transactionService.getHistoryForUser(42L, null,
                        LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1),
                        new BigDecimal("10"), new BigDecimal("1"), PageRequest.of(0, 10)));
        org.mockito.Mockito.verifyNoInteractions(transactionRepository);
    }

    @Test
    void getTransactionForUser_shouldResolveWalletAndMapOwnTransaction() {
        AccountEntity account = new AccountEntity("ACC-Test", "Test user");
        TransactionEntity transaction = new TransactionEntity(account, null, TransactionType.WITHDRAW,
                new BigDecimal("100.00"), new BigDecimal("49900.00"));
        when(accountRepository.findByUserId(42L)).thenReturn(java.util.Optional.of(account));
        when(transactionRepository.findByIdAndAccount(7L, account)).thenReturn(java.util.Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionForUser(42L, 7L);

        assertEquals(TransactionType.WITHDRAW, response.getTransactionType());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertNull(response.getRelatedAccountId());
        verify(accountRepository).findByUserId(42L);
        verify(transactionRepository).findByIdAndAccount(7L, account);
    }

    @Test
    void getTransactionForUser_shouldUseSameNotFoundResultForForeignOrMissingTransaction() {
        AccountEntity account = new AccountEntity("ACC-Test", "Test user");
        when(accountRepository.findByUserId(42L)).thenReturn(java.util.Optional.of(account));
        when(transactionRepository.findByIdAndAccount(7L, account)).thenReturn(java.util.Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransactionForUser(42L, 7L));
        verify(transactionRepository).findByIdAndAccount(7L, account);
    }
    
    @Test
    void recordTransaction_shoudSaveTransaction(){
        AccountEntity account = new AccountEntity("TEST", "TEST");
        transactionService.recordTransaction(account, null, TransactionType.DEPOSIT,new BigDecimal("10000.00"),new BigDecimal("10000.00"));
        verify(transactionRepository).save(any(TransactionEntity.class));


    }
}
