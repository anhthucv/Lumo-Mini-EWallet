package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.RiskEventEntity;
import com.chethu.paymentledgerservice.repository.RiskEventRepository;

@Service
public class RiskAuditService {
    private final RiskEventRepository riskEventRepository;

    public RiskAuditService(RiskEventRepository riskEventRepository) {
        this.riskEventRepository = riskEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(AccountEntity account, LimitOperationType operation, BigDecimal amount,
            RiskEvaluationResult result) {
        saveEvents(account, operation, amount, result, RiskDecision.REJECT);
    }

    public void recordFlagged(AccountEntity account, LimitOperationType operation, BigDecimal amount,
            RiskEvaluationResult result) {
        saveEvents(account, operation, amount, result, RiskDecision.FLAG);
    }

    private void saveEvents(AccountEntity account, LimitOperationType operation, BigDecimal amount,
            RiskEvaluationResult result, RiskDecision decision) {
        result.reasons().forEach(reason -> riskEventRepository.save(
                new RiskEventEntity(account, operation, amount, decision, reason)));
    }
}
