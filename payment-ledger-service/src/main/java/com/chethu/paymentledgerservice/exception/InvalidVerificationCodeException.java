package com.chethu.paymentledgerservice.exception;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("Verification code is invalid");
    }
}
