package com.chethu.paymentledgerservice.exception;

public class VerificationCodeNotFoundException extends RuntimeException {
    public VerificationCodeNotFoundException(String email) {
        super("No active verification code found for email: " + email);
    }
}
