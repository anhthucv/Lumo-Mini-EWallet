package com.chethu.paymentledgerservice.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(Long transactionId) {
        super("Transaction with " + transactionId + " not found");
    }
}
