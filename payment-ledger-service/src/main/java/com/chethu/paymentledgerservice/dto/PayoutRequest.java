package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PayoutRequest(
        @Positive @DecimalMin(value = "1000.00") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[0-9]{6,10}") String destinationBankIdentifier,
        @NotBlank @Pattern(regexp = "[0-9]{5,30}") String destinationAccountNumber) {
}
