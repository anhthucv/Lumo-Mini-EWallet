package com.chethu.paymentledgerservice.payment.payout;

import com.chethu.paymentledgerservice.domain.PayoutProviderType;

public record ProviderPayoutLookupResult(
        PayoutProviderType provider,
        String merchantReference,
        String providerReference,
        ProviderPayoutStatus status) {
}
