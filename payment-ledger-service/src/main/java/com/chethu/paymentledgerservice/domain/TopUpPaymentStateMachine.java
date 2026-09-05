package com.chethu.paymentledgerservice.domain;

import com.chethu.paymentledgerservice.exception.InvalidTopUpPaymentStatusTransitionException;

public final class TopUpPaymentStateMachine {
    private TopUpPaymentStateMachine() {
    }

    public static TopUpPaymentStatus transition(TopUpPaymentStatus current, TopUpPaymentStatus target) {
        if (current == target) {
            return current;
        }
        if (current == TopUpPaymentStatus.PENDING
                && (target == TopUpPaymentStatus.SUCCESS || target == TopUpPaymentStatus.CANCELLED)) {
            return target;
        }
        throw new InvalidTopUpPaymentStatusTransitionException(current, target);
    }
}
