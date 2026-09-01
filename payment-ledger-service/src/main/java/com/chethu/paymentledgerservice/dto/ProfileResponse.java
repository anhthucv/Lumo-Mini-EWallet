package com.chethu.paymentledgerservice.dto;

import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.entity.UserEntity;

public class ProfileResponse {
    private final Long userId;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final UserStatus status;
    private final LocalDateTime createdAt;

    public ProfileResponse(Long userId, String email, String fullName, UserRole role,
            UserStatus status, LocalDateTime createdAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ProfileResponse from(UserEntity user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getStatus(), user.getCreatedAt());
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
