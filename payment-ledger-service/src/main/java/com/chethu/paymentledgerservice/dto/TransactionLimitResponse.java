package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

public record TransactionLimitResponse(
        BigDecimal perTransactionLimit,
        BigDecimal dailyLimit,
        BigDecimal usedToday,
        BigDecimal remainingToday) {
}
