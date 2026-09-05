package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;

public record AdminLedgerEntryResponse(Long entryId, Long ledgerAccountId, String ledgerAccountCode,
        LedgerAccountType ledgerAccountType, LedgerEntryType direction, BigDecimal amount, LocalDateTime createdAt) {
    public static AdminLedgerEntryResponse from(LedgerEntryEntity entry) {
        return new AdminLedgerEntryResponse(entry.getId(), entry.getLedgerAccount().getId(), entry.getLedgerAccount().getCode(),
                entry.getLedgerAccount().getType(), entry.getEntryType(), entry.getAmount(), entry.getCreatedAt());
    }
}
