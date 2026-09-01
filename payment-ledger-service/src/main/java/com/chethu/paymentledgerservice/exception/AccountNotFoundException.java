package com.chethu.paymentledgerservice.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id){
        super("Account with " + id + " not found");
    }

    public AccountNotFoundException(String accountNumber) {
        super("Account with number " + accountNumber + " not found");
    }
}
