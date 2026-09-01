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

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
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
        
        assertThrows(AccountNotFoundException.class, () -> transactionService.getHistoryForUser(userId, pageable));
    }

    @Test 
    void getHistoryForUser_shouldReturnHistoryForResolvedWallet(){
        Long userId = 42L;
        Pageable pageable = PageRequest.of(0,5);

        AccountEntity account = new AccountEntity("ACC-Test","Test user");
        when(accountRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(account));
        TransactionEntity transaction = new TransactionEntity(account,null, TransactionType.DEPOSIT,new BigDecimal("50000.00"),new BigDecimal("50000.00"));

        Page<TransactionEntity> transactionPage = new PageImpl<>(List.of(transaction));
        when (transactionRepository.findByAccount(account, pageable)).thenReturn(transactionPage);

        Page<TransactionResponse>  result = transactionService.getHistoryForUser(userId, pageable);

        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
        assertNull( response.getRelatedAccountId());
        assertEquals(new BigDecimal("50000.00"), response.getAmount());   
        assertEquals(new BigDecimal("50000.00"), response.getBalanceAfterTransaction());           
        verify(accountRepository).findByUserId(userId);
        verify(transactionRepository).findByAccount(account, pageable);
    } 
    
    @Test
    void recordTransaction_shoudSaveTransaction(){
        AccountEntity account = new AccountEntity("TEST", "TEST");
        transactionService.recordTransaction(account, null, TransactionType.DEPOSIT,new BigDecimal("10000.00"),new BigDecimal("10000.00"));
        verify(transactionRepository).save(any(TransactionEntity.class));


    }
}
