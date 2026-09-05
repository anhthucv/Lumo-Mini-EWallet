package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;

public record TopUpResponse(Long id, long merchantOrderCode, BigDecimal amount, String currency,
        TopUpPaymentStatus status, PaymentProviderType provider, String checkoutUrl, LocalDateTime createdAt) {
    public static TopUpResponse from(TopUpPaymentEntity payment) {
        return new TopUpResponse(payment.getId(), payment.getMerchantOrderCode(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getProvider(), payment.getCheckoutUrl(),
                payment.getCreatedAt());
    }
}
