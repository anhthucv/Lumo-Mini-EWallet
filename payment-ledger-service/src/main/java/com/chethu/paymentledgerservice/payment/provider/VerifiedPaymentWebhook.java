package com.chethu.paymentledgerservice.payment.provider;

import java.math.BigDecimal;

public record VerifiedPaymentWebhook(
        PaymentProviderType provider,
        Long merchantOrderCode,
        BigDecimal amount,
        String currency,
        String providerReference,
        String providerTransactionReference,
        String providerCode,
        boolean successful) {
}
