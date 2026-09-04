package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.config.TransactionLimitProperties;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.TransactionLimitResponse;
import com.chethu.paymentledgerservice.dto.WalletLimitsResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.DailyTransactionLimitExceededException;
import com.chethu.paymentledgerservice.exception.PerTransactionLimitExceededException;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

@Service
public class TransactionLimitService {
    private final TransactionRepository transactionRepository;
    private final TransactionLimitProperties properties;

    public TransactionLimitService(TransactionRepository transactionRepository,
            TransactionLimitProperties properties) {
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    public void validate(AccountEntity account, LimitOperationType operation, BigDecimal amount) {
        TransactionLimitResponse limit = limitFor(account, operation);
        if (amount.compareTo(limit.perTransactionLimit()) > 0) {
            throw new PerTransactionLimitExceededException(limit.perTransactionLimit());
        }
        if (limit.usedToday().add(amount).compareTo(limit.dailyLimit()) > 0) {
            throw new DailyTransactionLimitExceededException(limit.remainingToday());
        }
    }

    public WalletLimitsResponse getWalletLimits(AccountEntity account) {
        return new WalletLimitsResponse(
                limitFor(account, LimitOperationType.DEPOSIT),
                limitFor(account, LimitOperationType.WITHDRAW),
                limitFor(account, LimitOperationType.TRANSFER));
    }

    public TransactionLimitResponse limitFor(AccountEntity account, LimitOperationType operation) {
        TransactionLimitProperties.OperationLimit configured = configuredLimit(operation);
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        BigDecimal used = transactionRepository.sumAmountForAccountAndTypeAndStatusBetween(
                account, transactionType(operation), TransactionStatus.SUCCESS, start, end);
        if (used == null) {
            used = BigDecimal.ZERO;
        }
        BigDecimal remaining = configured.getDaily().subtract(used).max(BigDecimal.ZERO);
        return new TransactionLimitResponse(configured.getPerTransaction(), configured.getDaily(), used, remaining);
    }

    private TransactionLimitProperties.OperationLimit configuredLimit(LimitOperationType operation) {
        return switch (operation) {
            case DEPOSIT -> properties.getDeposit();
            case WITHDRAW -> properties.getWithdraw();
            case TRANSFER -> properties.getTransfer();
        };
    }

    private TransactionType transactionType(LimitOperationType operation) {
        return switch (operation) {
            case DEPOSIT -> TransactionType.DEPOSIT;
            case WITHDRAW -> TransactionType.WITHDRAW;
            case TRANSFER -> TransactionType.TRANSFER_OUT;
        };
    }
}
