package com.chethu.paymentledgerservice.exception;

public class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException() {
        super("Verification code has expired");
    }
}
