package com.chethu.paymentledgerservice.payment.payout;

import com.chethu.paymentledgerservice.domain.PayoutProviderType;
import com.chethu.paymentledgerservice.domain.PayoutStatus;

public record ProviderPayoutResult(
        PayoutProviderType provider,
        String providerReference,
        PayoutStatus status) {
}
