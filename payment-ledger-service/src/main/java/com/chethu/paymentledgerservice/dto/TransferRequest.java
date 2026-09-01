package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransferRequest {
    @NotBlank(message = "Recipient account number must not be blank")
    private String recipientAccountNumber;
    @NotNull(message = "Amount must not be null")
    @DecimalMin (value="1.00",message="Amount must be at least 1 VNĐ")
    private BigDecimal amount;

    public String getRecipientAccountNumber() {
        return this.recipientAccountNumber;
    }

    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
