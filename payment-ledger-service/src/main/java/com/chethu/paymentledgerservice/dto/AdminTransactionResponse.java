package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;

public record AdminTransactionResponse(Long transactionId, LocalDateTime createdAt, TransactionType type,
        TransactionStatus status, BigDecimal amount, BigDecimal balanceAfterTransaction, Long journalId,
        Long userId, String userEmail, String userFullName, Long accountId, String maskedAccountNumber,
        Long relatedAccountId) {
    public static AdminTransactionResponse from(TransactionEntity tx) {
        AccountEntity account = tx.getAccount();
        return new AdminTransactionResponse(tx.getId(), tx.getCreatedAt(), tx.getTransactionType(), tx.getStatus(),
                tx.getAmount(), tx.getBalanceAfterTransaction(), tx.getJournal() == null ? null : tx.getJournal().getId(),
                account.getUser() == null ? null : account.getUser().getId(), account.getUser() == null ? null : account.getUser().getEmail(),
                account.getUser() == null ? null : account.getUser().getFullName(), account.getId(), mask(account.getAccountNumber()),
                tx.getRelatedAccount() == null ? null : tx.getRelatedAccount().getId());
    }
    public static String mask(String value) { return value == null || value.isBlank() ? null : "****" + value.substring(Math.max(0, value.length() - 4)); }
}
