package com.chethu.paymentledgerservice.exception;

public class InvalidAccountNumberException extends RuntimeException {
    public InvalidAccountNumberException() {
        super("Account number must not be blank");
    }
}
