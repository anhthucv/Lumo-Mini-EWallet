package com.chethu.paymentledgerservice.domain;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REVERSED
}
