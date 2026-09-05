package com.chethu.paymentledgerservice.exception;

public class InvalidPaymentWebhookException extends RuntimeException {
    public InvalidPaymentWebhookException() {
        super("Payment webhook could not be verified.");
    }

    public InvalidPaymentWebhookException(String message) {
        super(message);
    }
}
