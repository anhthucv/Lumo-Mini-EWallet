package com.chethu.paymentledgerservice.service;



import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
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
        log.info("Transaction recorded: accountId={}, type={}, amount={}",
            account.getId(), transactionType, amount
        );
    }

    public Page<TransactionResponse> getHistoryForUser(Long userId, Pageable pageable){
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Transaction history requested without an account for userId={}", userId);
                    return new AccountNotFoundException(userId);
                });
        log.info("Fetching transaction history for authenticated account: accountId={}", account.getId());
        Page<TransactionEntity> transactions = transactionRepository.findByAccount(account, pageable);
        Page<TransactionResponse> responses = transactions.map(this::toResponse);
        return responses;
    }



}
