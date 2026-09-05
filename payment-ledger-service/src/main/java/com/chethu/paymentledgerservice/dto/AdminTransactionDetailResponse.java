package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;

public record AdminTransactionDetailResponse(Long transactionId, TransactionType type, TransactionStatus status,
        BigDecimal amount, BigDecimal balanceAfterTransaction, LocalDateTime createdAt, Long userId, String userEmail,
        String userFullName, Long accountId, String maskedAccountNumber, Long relatedAccountId, Long journalId,
        String journalReference, LocalDateTime journalCreatedAt,
        List<AdminLedgerEntryResponse> ledgerEntries, AdminTopUpSummary topUp) {
    public record AdminTopUpSummary(Long topUpId, String provider, String status, String providerReference,
            Long merchantOrderCode, BigDecimal amount) {}
}
