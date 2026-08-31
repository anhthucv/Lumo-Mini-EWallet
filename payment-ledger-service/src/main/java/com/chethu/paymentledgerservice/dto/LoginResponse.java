package com.chethu.paymentledgerservice.dto;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;

public class LoginResponse {
    private final Long userId;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final UserStatus status;
    private final String accessToken;
    private final String tokenType;
    private final Long expiresIn;

    public LoginResponse(Long userId, String email, String fullName, UserRole role, UserStatus status,
            String accessToken, String tokenType, Long expiresIn) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
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

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}
