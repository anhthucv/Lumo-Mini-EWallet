package com.chethu.paymentledgerservice.payment.provider;

public record PaymentCheckoutResult(
        PaymentProviderType provider,
        long merchantOrderCode,
        String providerReference,
        String checkoutUrl) {
}
