package com.chethu.paymentledgerservice.service;



import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    public TransactionService (TransactionRepository transactionRepository, AccountRepository accountRepository){
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    private TransactionResponse toResponse(TransactionEntity transaction){
        TransactionResponse response = new TransactionResponse(
            transaction.getId(), 
            transaction.getAccount().getId(),
            transaction.getRelatedAccount() != null ? transaction.getRelatedAccount().getId() : null,
            transaction.getTransactionType(), 
            transaction.getAmount(), 
            transaction.getBalanceAfterTransaction(),
            transaction.getCreatedAt()
        );
        return response;
    }

    public void recordTransaction(AccountEntity account, AccountEntity relatedAccount, TransactionType transactionType, BigDecimal amount,BigDecimal balance){
        TransactionEntity transaction = new TransactionEntity (account,relatedAccount, transactionType, amount,balance);
        transactionRepository.save(transaction);
    }

    public Page<TransactionResponse> getHistoryByAccount(Long accountId, Pageable pageable){
        if (!accountRepository.existsById(accountId)) throw new AccountNotFoundException(accountId);
        Page<TransactionEntity> transactions = transactionRepository.getHistoryByAccount(accountId, pageable);
        Page<TransactionResponse> responses = transactions.map(this::toResponse);
        return responses;
    }



}
