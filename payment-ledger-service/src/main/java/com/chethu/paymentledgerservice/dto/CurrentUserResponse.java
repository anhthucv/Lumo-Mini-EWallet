package com.chethu.paymentledgerservice.dto;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;

public class CurrentUserResponse {
    private final Long userId;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final UserStatus status;

    public CurrentUserResponse(Long userId, String email, String fullName, UserRole role, UserStatus status) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    public static CurrentUserResponse from(AuthenticatedUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.userId(),
                principal.email(),
                principal.fullName(),
                principal.role(),
                principal.status());
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
}
