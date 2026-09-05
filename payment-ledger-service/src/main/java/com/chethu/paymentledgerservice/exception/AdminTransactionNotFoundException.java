package com.chethu.paymentledgerservice.exception;
public class AdminTransactionNotFoundException extends RuntimeException {
    public AdminTransactionNotFoundException(Long id) { super("Transaction with " + id + " not found"); }
}
