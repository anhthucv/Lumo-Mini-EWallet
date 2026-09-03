package com.chethu.paymentledgerservice.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("Idempotency-Key was already used with different request data");
    }
}
