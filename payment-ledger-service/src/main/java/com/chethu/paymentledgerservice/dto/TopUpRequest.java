package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TopUpRequest(
        @NotNull(message = "Amount must not be null")
        @DecimalMin(value = "1000.00", message = "Top-up amount must be at least 1,000 VND")
        BigDecimal amount) {
}
