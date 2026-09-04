package com.chethu.paymentledgerservice.exception;

public class RiskRejectedException extends RuntimeException {
    public RiskRejectedException() {
        super("Transaction was rejected by risk controls.");
    }
}
