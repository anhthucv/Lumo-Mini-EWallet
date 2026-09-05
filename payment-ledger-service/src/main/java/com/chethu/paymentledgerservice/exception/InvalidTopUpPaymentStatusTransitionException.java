package com.chethu.paymentledgerservice.exception;

import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;

public class InvalidTopUpPaymentStatusTransitionException extends RuntimeException {
    public InvalidTopUpPaymentStatusTransitionException(TopUpPaymentStatus current, TopUpPaymentStatus target) {
        super("Top-up payment cannot transition from " + current + " to " + target);
    }
}
