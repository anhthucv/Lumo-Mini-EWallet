package com.chethu.paymentledgerservice.exception;

import com.chethu.paymentledgerservice.domain.TransactionStatus;

public class InvalidTransactionStatusTransitionException extends RuntimeException {
    public InvalidTransactionStatusTransitionException(TransactionStatus current, TransactionStatus requested) {
        super("Cannot transition transaction from " + current + " to " + requested);
    }
}
