package com.chethu.paymentledgerservice.exception;

public class AdminUserOperationForbiddenException extends RuntimeException {
    public AdminUserOperationForbiddenException(String message) {
        super(message);
    }
}
