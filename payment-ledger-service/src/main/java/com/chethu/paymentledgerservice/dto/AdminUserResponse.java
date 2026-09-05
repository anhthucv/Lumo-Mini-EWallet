package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;

public record AdminUserResponse(Long userId, String email, String fullName, UserRole role, UserStatus status,
        LocalDateTime createdAt, Long accountId, String accountNumberSummary, AccountStatus accountStatus,
        BigDecimal balance) {
    public static AdminUserResponse from(UserEntity user, AccountEntity account) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getStatus(), user.getCreatedAt(), account == null ? null : account.getId(),
                account == null ? null : mask(account.getAccountNumber()), account == null ? null : account.getStatus(),
                account == null ? null : account.getBalance());
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return null;
        return "****" + accountNumber.substring(Math.max(0, accountNumber.length() - 4));
    }
}
