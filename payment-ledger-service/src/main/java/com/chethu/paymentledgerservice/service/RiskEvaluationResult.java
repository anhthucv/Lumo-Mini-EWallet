package com.chethu.paymentledgerservice.service;

import java.util.List;

import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.RiskReasonCode;

public record RiskEvaluationResult(RiskDecision decision, List<RiskReasonCode> reasons) {
    public RiskEvaluationResult {
        reasons = List.copyOf(reasons);
    }
}
