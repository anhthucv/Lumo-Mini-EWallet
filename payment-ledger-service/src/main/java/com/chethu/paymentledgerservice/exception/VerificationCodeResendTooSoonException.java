package com.chethu.paymentledgerservice.exception;

public class VerificationCodeResendTooSoonException extends RuntimeException {
    public VerificationCodeResendTooSoonException() {
        super("Please wait 60 seconds before requesting a new verification code");
    }
}
