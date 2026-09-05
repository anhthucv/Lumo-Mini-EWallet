package com.chethu.paymentledgerservice.payment.provider;

import java.math.BigDecimal;

public record PaymentCheckoutRequest(long merchantOrderCode, BigDecimal amount, String currency, String description) {
}
