package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

public class TransferResponse {
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private BigDecimal fromBalance;
    private BigDecimal toBalance;
    private String status;
    public TransferResponse(
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            BigDecimal fromBalance,
            BigDecimal toBalance,
            String status
    ) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.fromBalance = fromBalance;
        this.toBalance = toBalance;
        this.status = status;
    }

    public Long getFromAccountId() {
        return this.fromAccountId;
    }

    public Long getToAccountId() {
        return this.toAccountId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getFromBalance() {
        return this.fromBalance;
    }

    public BigDecimal getToBalance() {
        return this.toBalance;
    }

    public String getStatus() {
        return this.status;
    }

}
