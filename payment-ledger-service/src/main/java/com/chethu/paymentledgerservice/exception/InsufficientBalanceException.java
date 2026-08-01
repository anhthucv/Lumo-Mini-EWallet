package com.chethu.paymentledgerservice.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException (){
        super("Insufficient balance");
    }
}
