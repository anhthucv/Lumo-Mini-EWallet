package com.chethu.paymentledgerservice.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.InvalidTransactionStatusTransitionException;

class TransactionStatusTest {
    private final AccountEntity account = new AccountEntity("ACC-STATUS", "Status User");

    @Test
    void internalTransaction_defaultsToSuccess() {
        TransactionEntity transaction = transaction(TransactionStatus.SUCCESS);

        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
    }

    @Test
    void allowedTransitions_shouldBeAccepted() {
        assertTransition(TransactionStatus.PENDING, TransactionStatus.PROCESSING);
        assertTransition(TransactionStatus.PENDING, TransactionStatus.SUCCESS);
        assertTransition(TransactionStatus.PENDING, TransactionStatus.FAILED);
        assertTransition(TransactionStatus.PENDING, TransactionStatus.CANCELLED);
        assertTransition(TransactionStatus.PROCESSING, TransactionStatus.SUCCESS);
        assertTransition(TransactionStatus.PROCESSING, TransactionStatus.FAILED);
        assertTransition(TransactionStatus.PROCESSING, TransactionStatus.CANCELLED);
        assertTransition(TransactionStatus.SUCCESS, TransactionStatus.REVERSED);
    }

    @Test
    void terminalAndBackwardTransitions_shouldBeRejected() {
        assertInvalid(TransactionStatus.SUCCESS, TransactionStatus.PROCESSING);
        assertInvalid(TransactionStatus.FAILED, TransactionStatus.SUCCESS);
        assertInvalid(TransactionStatus.CANCELLED, TransactionStatus.PROCESSING);
        assertInvalid(TransactionStatus.REVERSED, TransactionStatus.PENDING);
    }

    @Test
    void statusEnum_shouldExposeExactlyRequiredValues() {
        assertEquals(java.util.List.of(TransactionStatus.PENDING, TransactionStatus.PROCESSING,
                TransactionStatus.SUCCESS, TransactionStatus.FAILED, TransactionStatus.CANCELLED,
                TransactionStatus.REVERSED), java.util.List.of(TransactionStatus.values()));
    }

    private void assertTransition(TransactionStatus current, TransactionStatus target) {
        TransactionEntity transaction = transaction(current);
        assertDoesNotThrow(() -> transaction.transitionTo(target));
        assertEquals(target, transaction.getStatus());
    }

    private void assertInvalid(TransactionStatus current, TransactionStatus target) {
        TransactionEntity transaction = transaction(current);
        assertThrows(InvalidTransactionStatusTransitionException.class,
                () -> transaction.transitionTo(target));
        assertEquals(current, transaction.getStatus());
    }

    private TransactionEntity transaction(TransactionStatus status) {
        return new TransactionEntity(account, null, TransactionType.DEPOSIT,
                new BigDecimal("100.00"), new BigDecimal("100.00"), status);
    }
}
