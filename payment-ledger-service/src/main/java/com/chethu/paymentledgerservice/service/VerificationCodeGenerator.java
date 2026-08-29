package com.chethu.paymentledgerservice.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class VerificationCodeGenerator {
    private static final int CODE_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }
}
