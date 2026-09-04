package com.chethu.paymentledgerservice.exception;

import java.math.BigDecimal;

public class DailyTransactionLimitExceededException extends RuntimeException {
    public DailyTransactionLimitExceededException(BigDecimal remaining) {
        super("Amount exceeds the remaining daily limit of " + remaining);
    }
}
