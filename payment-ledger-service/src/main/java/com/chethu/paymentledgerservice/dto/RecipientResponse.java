package com.chethu.paymentledgerservice.dto;

import com.chethu.paymentledgerservice.entity.AccountEntity;

public class RecipientResponse {
    private final String accountNumber;
    private final String ownerName;

    public RecipientResponse(String accountNumber, String ownerName) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
    }

    public static RecipientResponse from(AccountEntity account) {
        return new RecipientResponse(account.getAccountNumber(), account.getOwnerName());
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
