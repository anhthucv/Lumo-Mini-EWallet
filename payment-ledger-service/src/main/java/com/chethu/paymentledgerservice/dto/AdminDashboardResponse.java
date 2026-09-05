package com.chethu.paymentledgerservice.dto;

import java.math.BigDecimal;
import java.util.List;
public record AdminDashboardResponse(UserMetrics users, WalletMetrics wallets, TransactionMetrics transactions,
        List<AdminTransactionResponse> recentActivity) {
    public record UserMetrics(long total, long active, long locked) {}
    public record WalletMetrics(long total) {}
    public record TransactionMetrics(long total, long successful, BigDecimal depositVolume, BigDecimal transferVolume) {}
}
