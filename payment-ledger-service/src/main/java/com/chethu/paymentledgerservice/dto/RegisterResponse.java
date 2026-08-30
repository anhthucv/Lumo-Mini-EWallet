package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;

public class RegisterResponse {
    private final Long userId;
    private final String email;
    private final String fullName;
    private final Long accountId;
    private final String accountNumber;
    private final BigDecimal balance;
    private final UserRole role;
    private final UserStatus userStatus;
    private final AccountStatus accountStatus;

    public RegisterResponse(Long userId, String email, String fullName, Long accountId, String accountNumber,
            BigDecimal balance, UserRole role, UserStatus userStatus, AccountStatus accountStatus) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.role = role;
        this.userStatus = userStatus;
        this.accountStatus = accountStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }
}
