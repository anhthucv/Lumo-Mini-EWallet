package com.chethu.paymentledgerservice.exception;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException() {
        super("Account is not active");
    }
}
