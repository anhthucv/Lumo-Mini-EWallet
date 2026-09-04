package com.chethu.paymentledgerservice.dto;

import java.time.LocalDateTime;

import com.chethu.paymentledgerservice.entity.BeneficiaryEntity;

public record BeneficiaryResponse(
        Long id,
        String accountNumber,
        String recipientOwnerName,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static BeneficiaryResponse from(BeneficiaryEntity beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getBeneficiaryAccount().getAccountNumber(),
                beneficiary.getBeneficiaryAccount().getOwnerName(),
                beneficiary.getNickname(),
                beneficiary.getCreatedAt(),
                beneficiary.getUpdatedAt());
    }
}
