package com.chethu.paymentledgerservice.exception;

public class TopUpPaymentNotFoundException extends RuntimeException {
    public TopUpPaymentNotFoundException(Long id) {
        super("Top-up payment " + id + " was not found");
    }
}
