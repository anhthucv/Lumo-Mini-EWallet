package com.chethu.paymentledgerservice.exception;

public class InvalidTransactionFilterException extends RuntimeException {
    public InvalidTransactionFilterException(String message) {
        super(message);
    }
}
