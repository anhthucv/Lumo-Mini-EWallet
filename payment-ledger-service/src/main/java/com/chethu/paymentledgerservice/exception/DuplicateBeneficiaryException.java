package com.chethu.paymentledgerservice.exception;

public class DuplicateBeneficiaryException extends RuntimeException {
    public DuplicateBeneficiaryException() {
        super("This beneficiary is already saved");
    }
}
