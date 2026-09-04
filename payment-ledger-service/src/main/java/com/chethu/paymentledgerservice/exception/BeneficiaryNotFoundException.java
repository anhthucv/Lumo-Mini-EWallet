package com.chethu.paymentledgerservice.exception;

public class BeneficiaryNotFoundException extends RuntimeException {
    public BeneficiaryNotFoundException(Long id) {
        super("Beneficiary with id " + id + " not found");
    }
}
