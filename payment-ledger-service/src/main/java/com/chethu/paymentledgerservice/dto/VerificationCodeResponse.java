package com.chethu.paymentledgerservice.dto;

public class VerificationCodeResponse {
    private final String message;

    public VerificationCodeResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
