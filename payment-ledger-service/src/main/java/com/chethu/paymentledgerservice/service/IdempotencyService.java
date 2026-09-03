package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.exception.IdempotencyConflictException;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;

@Service
public class IdempotencyService {
    private static final int MAX_KEY_LENGTH = 100;

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        String key = rawKey.trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key must contain 1 to 100 characters");
        }
        return key;
    }

    public IdempotencyRecordEntity findExisting(AccountEntity account, IdempotencyOperationType operationType,
            String rawKey, BigDecimal amount, String recipientAccountNumber) {
        String key = normalizeKey(rawKey);
        if (key == null) {
            return null;
        }
        return repository.findByAccountAndIdempotencyKey(account, key)
                .map(existing -> {
                    if (existing.getOperationType() != operationType
                            || existing.getRequestAmount().compareTo(amount) != 0
                            || !sameRecipient(existing.getRecipientAccountNumber(), recipientAccountNumber)) {
                        throw new IdempotencyConflictException();
                    }
                    return existing;
                }).orElse(null);
    }

    public void saveCompleted(AccountEntity account, IdempotencyOperationType operationType,
            String rawKey, BigDecimal amount, String recipientAccountNumber,
            BigDecimal resultBalance, JournalEntity journal) {
        String key = normalizeKey(rawKey);
        if (key != null) {
            repository.save(new IdempotencyRecordEntity(account, operationType, key, amount,
                    recipientAccountNumber, resultBalance, journal));
        }
    }

    private boolean sameRecipient(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
