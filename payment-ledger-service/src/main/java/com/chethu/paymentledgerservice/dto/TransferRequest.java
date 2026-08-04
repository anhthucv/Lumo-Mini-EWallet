package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class TransferRequest {
    @NotNull (message = "Sender account id must not be null")
    private Long fromAccountId;
    @NotNull (message = "Receiver account id must not be null")
    private Long toAccountId;
    @NotNull(message = "Amount must not be null")
    @DecimalMin (value="1.00",message="Amount must be at least 1 VNĐ")
    private BigDecimal amount;
    
    public Long getFromAccountId() {
        return this.fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return this.toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


}
