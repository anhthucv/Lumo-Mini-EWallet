package com.chethu.paymentledgerservice.payment.payout;

import java.util.Optional;

public interface PayoutProvider {
    ProviderPayoutResult createPayout(ProviderPayoutRequest request);

    Optional<ProviderPayoutLookupResult> findByMerchantReference(String merchantReference);
}
