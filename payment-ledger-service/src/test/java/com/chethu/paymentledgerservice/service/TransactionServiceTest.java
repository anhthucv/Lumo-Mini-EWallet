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
import static org.mockito.Mockito.never;
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
    void getHistoryByAccount_shouldThrowException_whenAccountNotFound(){
        Long accountId = 999L;
        Pageable pageable = PageRequest.of(0,5);
        when (accountRepository.existsById(accountId)).thenReturn(false);
        
        assertThrows(AccountNotFoundException.class, () -> transactionService.getHistoryByAccount(accountId, pageable));

        verify(transactionRepository,never()).getHistoryByAccount(accountId, pageable);
    }

    @Test 
    void getHistoryByAccount_shouldReturnHistory_whenAccountExists(){
        Long accountId = 1L;
        Pageable pageable = PageRequest.of(0,5);
        when (accountRepository.existsById(accountId)).thenReturn(true);

        AccountEntity account = new AccountEntity("ACC-Test","Test user");
        TransactionEntity transaction = new TransactionEntity(account,null, TransactionType.DEPOSIT,new BigDecimal("50000.00"),new BigDecimal("50000.00"));

        Page<TransactionEntity> transactionPage = new PageImpl<>(List.of(transaction));
        when (transactionRepository.getHistoryByAccount(accountId, pageable)).thenReturn(transactionPage);

        Page<TransactionResponse>  result = transactionService.getHistoryByAccount(accountId, pageable);

        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
        assertNull( response.getRelatedAccountId());
        assertEquals(new BigDecimal("50000.00"), response.getAmount());   
        assertEquals(new BigDecimal("50000.00"), response.getBalanceAfterTransaction());           
        verify(transactionRepository).getHistoryByAccount(accountId, pageable);
    } 
    
    @Test
    void recordTransaction_shoudSaveTransaction(){
        AccountEntity account = new AccountEntity("TEST", "TEST");
        transactionService.recordTransaction(account, null, TransactionType.DEPOSIT,new BigDecimal("10000.00"),new BigDecimal("10000.00"));
        verify(transactionRepository).save(any(TransactionEntity.class));


    }
}
