package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.chethu.paymentledgerservice.config.RiskRuleProperties;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.RiskReasonCode;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class RiskEvaluationServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    private RiskRuleProperties properties;
    private RiskEvaluationService service;
    private AccountEntity account;

    @BeforeEach
    void setUp() {
        properties = new RiskRuleProperties();
        service = new RiskEvaluationService(transactionRepository, properties);
        account = new AccountEntity("ACC-RISK", "Risk Owner");
    }

    @Test
    void amountBelowThreshold_withoutVelocity_shouldAllow() {
        RiskEvaluationResult result = service.evaluate(account, LimitOperationType.DEPOSIT,
                new BigDecimal("9999999.99"));

        assertEquals(RiskDecision.ALLOW, result.decision());
        assertEquals(List.of(), result.reasons());
        verify(transactionRepository, never()).countByAccountAndTypesAndStatusSince(any(), any(), any(), any());
    }

    @Test
    void amountAtThreshold_shouldFlagLargeAmount() {
        RiskEvaluationResult result = service.evaluate(account, LimitOperationType.DEPOSIT,
                new BigDecimal("10000000.00"));

        assertEquals(RiskDecision.FLAG, result.decision());
        assertEquals(List.of(RiskReasonCode.LARGE_AMOUNT), result.reasons());
    }

    @Test
    void fifthExistingOutgoingTransaction_shouldRejectNewOutgoingRequest() {
        when(transactionRepository.countByAccountAndTypesAndStatusSince(
                eq(account), eq(List.of(TransactionType.WITHDRAW, TransactionType.TRANSFER_OUT)),
                eq(TransactionStatus.SUCCESS), any(LocalDateTime.class))).thenReturn(5L);

        RiskEvaluationResult result = service.evaluate(account, LimitOperationType.WITHDRAW,
                new BigDecimal("100.00"));

        assertEquals(RiskDecision.REJECT, result.decision());
        assertEquals(List.of(RiskReasonCode.RAPID_OUTGOING_ACTIVITY), result.reasons());
    }

    @Test
    void largeAmountAndVelocity_shouldPreferRejectAndPreserveBothReasons() {
        when(transactionRepository.countByAccountAndTypesAndStatusSince(
                eq(account), eq(List.of(TransactionType.WITHDRAW, TransactionType.TRANSFER_OUT)),
                eq(TransactionStatus.SUCCESS), any(LocalDateTime.class))).thenReturn(5L);

        RiskEvaluationResult result = service.evaluate(account, LimitOperationType.TRANSFER,
                new BigDecimal("10000000.00"));

        assertEquals(RiskDecision.REJECT, result.decision());
        assertEquals(List.of(RiskReasonCode.LARGE_AMOUNT, RiskReasonCode.RAPID_OUTGOING_ACTIVITY), result.reasons());
    }

    @Test
    void transferInAndDeposit_shouldNotQueryOutgoingVelocity() {
        service.evaluate(account, LimitOperationType.DEPOSIT, new BigDecimal("100.00"));

        verify(transactionRepository, never()).countByAccountAndTypesAndStatusSince(any(), any(), any(), any());
    }
}
