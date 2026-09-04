package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.config.RiskRuleProperties;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.RiskReasonCode;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

@Service
public class RiskEvaluationService {
    private static final List<TransactionType> OUTGOING_TYPES = List.of(
            TransactionType.WITHDRAW, TransactionType.TRANSFER_OUT);

    private final TransactionRepository transactionRepository;
    private final RiskRuleProperties properties;

    public RiskEvaluationService(TransactionRepository transactionRepository, RiskRuleProperties properties) {
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    public RiskEvaluationResult evaluate(AccountEntity account, LimitOperationType operation, BigDecimal amount) {
        List<RiskReasonCode> reasons = new ArrayList<>();
        if (amount.compareTo(properties.getLargeAmountThreshold()) >= 0) {
            reasons.add(RiskReasonCode.LARGE_AMOUNT);
        }

        if (isOutgoing(operation)) {
            LocalDateTime start = LocalDateTime.now().minusMinutes(properties.getRapidOutgoingWindowMinutes());
            long count = transactionRepository.countByAccountAndTypesAndStatusSince(
                    account, OUTGOING_TYPES, TransactionStatus.SUCCESS, start);
            if (count >= properties.getRapidOutgoingMaxSuccessful()) {
                reasons.add(RiskReasonCode.RAPID_OUTGOING_ACTIVITY);
            }
        }

        RiskDecision decision = reasons.contains(RiskReasonCode.RAPID_OUTGOING_ACTIVITY)
                ? RiskDecision.REJECT
                : reasons.isEmpty() ? RiskDecision.ALLOW : RiskDecision.FLAG;
        return new RiskEvaluationResult(decision, reasons);
    }

    private boolean isOutgoing(LimitOperationType operation) {
        return operation == LimitOperationType.WITHDRAW || operation == LimitOperationType.TRANSFER;
    }
}
