package com.chethu.paymentledgerservice.payment.provider;

import java.math.BigDecimal;

public record ProviderPaymentStatusResult(
        PaymentProviderType provider,
        Long merchantOrderCode,
        BigDecimal amount,
        String currency,
        String providerReference,
        String providerTransactionReference,
        ProviderPaymentStatus status) {
}
