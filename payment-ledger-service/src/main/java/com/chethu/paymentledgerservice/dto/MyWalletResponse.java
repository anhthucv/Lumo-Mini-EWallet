package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.entity.AccountEntity;

public class MyWalletResponse {
    private final Long accountId;
    private final String accountNumber;
    private final String ownerName;
    private final BigDecimal balance;
    private final AccountStatus status;

    public MyWalletResponse(Long accountId, String accountNumber, String ownerName, BigDecimal balance,
            AccountStatus status) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
        this.status = status;
    }

    public static MyWalletResponse from(AccountEntity account) {
        return new MyWalletResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance(),
                account.getStatus());
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
