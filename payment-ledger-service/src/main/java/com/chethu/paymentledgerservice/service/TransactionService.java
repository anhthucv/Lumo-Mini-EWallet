package com.chethu.paymentledgerservice.service;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.InvalidTransactionFilterException;
import com.chethu.paymentledgerservice.exception.TransactionNotFoundException;
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
            transaction.getCreatedAt(),
            transaction.getStatus()
        );
        return response;
    }

    public void recordTransaction(AccountEntity account, AccountEntity relatedAccount, TransactionType transactionType, BigDecimal amount,BigDecimal balance){
        recordTransaction(account, relatedAccount, transactionType, amount, balance, null);
    }

    public void recordTransaction(AccountEntity account, AccountEntity relatedAccount, TransactionType transactionType,
            BigDecimal amount, BigDecimal balance, JournalEntity journal) {
        TransactionEntity transaction = new TransactionEntity (account,relatedAccount, transactionType, amount,balance);
        if (journal != null) {
            transaction.associateJournal(journal);
        }
        transactionRepository.save(transaction);
        log.info("Transaction recorded: accountId={}, type={}, amount={}",
            account.getId(), transactionType, amount
        );
    }

    public Page<TransactionResponse> getHistoryForUser(Long userId, TransactionType type,
            LocalDate fromDate, LocalDate toDate, BigDecimal minAmount, BigDecimal maxAmount,
            Pageable pageable){
        return getHistoryForUser(userId, type, null, fromDate, toDate, minAmount, maxAmount, pageable);
    }

    public Page<TransactionResponse> getHistoryForUser(Long userId, TransactionType type, TransactionStatus status,
            LocalDate fromDate, LocalDate toDate, BigDecimal minAmount, BigDecimal maxAmount,
            Pageable pageable){
        validateFilters(fromDate, toDate, minAmount, maxAmount);
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Transaction history requested without an account for userId={}", userId);
                    return new AccountNotFoundException(userId);
                });
        log.info("Fetching transaction history for authenticated account: accountId={}", account.getId());
        Specification<TransactionEntity> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("account"), account);
        if (type != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("transactionType"), type));
        }
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (fromDate != null) {
            LocalDateTime from = fromDate.atStartOfDay();
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (toDate != null) {
            LocalDateTime exclusiveEnd = toDate.plusDays(1).atStartOfDay();
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThan(root.get("createdAt"), exclusiveEnd));
        }
        if (minAmount != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
        }
        if (maxAmount != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
        }

        Page<TransactionEntity> transactions = transactionRepository.findAll(specification, pageable);
        Page<TransactionResponse> responses = transactions.map(this::toResponse);
        return responses;
    }

    public TransactionResponse getTransactionForUser(Long userId, Long transactionId) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        TransactionEntity transaction = transactionRepository.findByIdAndAccount(transactionId, account)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return toResponse(transaction);
    }

    private void validateFilters(LocalDate fromDate, LocalDate toDate,
            BigDecimal minAmount, BigDecimal maxAmount) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidTransactionFilterException("fromDate must be on or before toDate");
        }
        if (minAmount != null && minAmount.signum() < 0) {
            throw new InvalidTransactionFilterException("minAmount must not be negative");
        }
        if (maxAmount != null && maxAmount.signum() < 0) {
            throw new InvalidTransactionFilterException("maxAmount must not be negative");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new InvalidTransactionFilterException("minAmount must be less than or equal to maxAmount");
        }
    }



}
