package com.chethu.paymentledgerservice.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "transaction.limits")
public class TransactionLimitProperties {
    private OperationLimit deposit = new OperationLimit(new BigDecimal("50000000.00"),
            new BigDecimal("100000000.00"));
    private OperationLimit withdraw = new OperationLimit(new BigDecimal("20000000.00"),
            new BigDecimal("50000000.00"));
    private OperationLimit transfer = new OperationLimit(new BigDecimal("50000000.00"),
            new BigDecimal("100000000.00"));

    public OperationLimit getDeposit() { return deposit; }
    public void setDeposit(OperationLimit deposit) { this.deposit = deposit; }
    public OperationLimit getWithdraw() { return withdraw; }
    public void setWithdraw(OperationLimit withdraw) { this.withdraw = withdraw; }
    public OperationLimit getTransfer() { return transfer; }
    public void setTransfer(OperationLimit transfer) { this.transfer = transfer; }

    public static class OperationLimit {
        private BigDecimal perTransaction;
        private BigDecimal daily;

        public OperationLimit() { }

        public OperationLimit(BigDecimal perTransaction, BigDecimal daily) {
            this.perTransaction = perTransaction;
            this.daily = daily;
        }

        public BigDecimal getPerTransaction() { return perTransaction; }
        public void setPerTransaction(BigDecimal perTransaction) { this.perTransaction = perTransaction; }
        public BigDecimal getDaily() { return daily; }
        public void setDaily(BigDecimal daily) { this.daily = daily; }
    }
}
