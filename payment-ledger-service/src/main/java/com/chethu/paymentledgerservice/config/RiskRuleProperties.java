package com.chethu.paymentledgerservice.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk.rules")
public class RiskRuleProperties {
    private BigDecimal largeAmountThreshold = new BigDecimal("10000000.00");
    private long rapidOutgoingWindowMinutes = 10;
    private long rapidOutgoingMaxSuccessful = 5;

    public BigDecimal getLargeAmountThreshold() { return largeAmountThreshold; }
    public void setLargeAmountThreshold(BigDecimal largeAmountThreshold) {
        this.largeAmountThreshold = largeAmountThreshold;
    }

    public long getRapidOutgoingWindowMinutes() { return rapidOutgoingWindowMinutes; }
    public void setRapidOutgoingWindowMinutes(long rapidOutgoingWindowMinutes) {
        this.rapidOutgoingWindowMinutes = rapidOutgoingWindowMinutes;
    }

    public long getRapidOutgoingMaxSuccessful() { return rapidOutgoingMaxSuccessful; }
    public void setRapidOutgoingMaxSuccessful(long rapidOutgoingMaxSuccessful) {
        this.rapidOutgoingMaxSuccessful = rapidOutgoingMaxSuccessful;
    }
}
