package com.chethu.paymentledgerservice.payment.provider;

public interface PaymentProvider {
    PaymentCheckoutResult createCheckout(PaymentCheckoutRequest request);

    VerifiedPaymentWebhook verifyWebhook(String rawPayload);

    ProviderPaymentStatusResult getPaymentStatus(long merchantOrderCode);
}
