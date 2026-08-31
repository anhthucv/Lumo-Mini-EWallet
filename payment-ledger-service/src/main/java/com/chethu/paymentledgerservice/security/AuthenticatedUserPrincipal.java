package com.chethu.paymentledgerservice.security;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;

public record AuthenticatedUserPrincipal(
        Long userId,
        String email,
        String fullName,
        UserRole role,
        UserStatus status) {
}
