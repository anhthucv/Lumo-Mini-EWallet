package com.chethu.paymentledgerservice.exception;

import java.math.BigDecimal;

public class PerTransactionLimitExceededException extends RuntimeException {
    public PerTransactionLimitExceededException(BigDecimal limit) {
        super("Amount exceeds the per-transaction limit of " + limit);
    }
}
