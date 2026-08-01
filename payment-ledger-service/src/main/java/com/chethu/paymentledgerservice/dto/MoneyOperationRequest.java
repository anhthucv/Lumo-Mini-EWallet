package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class MoneyOperationRequest {
    @NotNull(message="Amount must not be null")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1 VNĐ")
    private BigDecimal amount;

    public BigDecimal getAmount(){
        return this.amount;
    }

    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }
}
