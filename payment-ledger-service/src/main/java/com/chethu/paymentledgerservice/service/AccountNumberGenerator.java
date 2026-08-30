package com.chethu.paymentledgerservice.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.chethu.paymentledgerservice.repository.AccountRepository;

@Component
public class AccountNumberGenerator {
    private static final int RANDOM_DIGITS = 12;
    private static final int MAX_ATTEMPTS = 10;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generateCandidate();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }

    protected String generateCandidate() {
        long value = secureRandom.nextLong(1_000_000_000_000L);
        return "ACC-" + String.format("%0" + RANDOM_DIGITS + "d", value);
    }
}
