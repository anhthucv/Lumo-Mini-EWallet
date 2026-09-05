package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.PayoutProviderType;
import com.chethu.paymentledgerservice.domain.PayoutStatus;
import com.chethu.paymentledgerservice.entity.PayoutEntity;

public record PayoutResponse(Long id, BigDecimal amount, String currency, PayoutStatus status,
        PayoutProviderType provider, String destinationBankIdentifier, String destinationAccountSummary,
        LocalDateTime createdAt) {
    public static PayoutResponse from(PayoutEntity payout) {
        return new PayoutResponse(payout.getId(), payout.getAmount(), payout.getCurrency(), payout.getStatus(),
                payout.getProvider(), payout.getDestinationBankIdentifier(), payout.getDestinationAccountSummary(),
                payout.getCreatedAt());
    }
}
