package com.chethu.paymentledgerservice.payment.payout;

import java.math.BigDecimal;

public record ProviderPayoutRequest(
        String merchantReference,
        BigDecimal amount,
        String currency,
        String description,
        String destinationBankIdentifier,
        String destinationAccountNumber,
        String idempotencyKey) {
}
