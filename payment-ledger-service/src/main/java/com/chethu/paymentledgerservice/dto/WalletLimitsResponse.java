package com.chethu.paymentledgerservice.dto;

public record WalletLimitsResponse(
        TransactionLimitResponse deposit,
        TransactionLimitResponse withdraw,
        TransactionLimitResponse transfer) {
}
