package com.chethu.paymentledgerservice.exception;

public class UserLockedException extends RuntimeException {
    public UserLockedException() {
        super("User account is locked");
    }
}
