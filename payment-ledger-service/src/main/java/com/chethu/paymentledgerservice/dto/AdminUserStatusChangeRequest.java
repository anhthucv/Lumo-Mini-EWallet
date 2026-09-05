package com.chethu.paymentledgerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserStatusChangeRequest(
        @NotBlank @Size(max = 255) String reason) {
}
