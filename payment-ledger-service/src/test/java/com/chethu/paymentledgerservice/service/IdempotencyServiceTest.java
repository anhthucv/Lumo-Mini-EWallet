package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import jakarta.persistence.Table;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.exception.IdempotencyConflictException;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;

class IdempotencyServiceTest {
    @Test
    void absentKeyPreservesCurrentBehaviorAndBlankOrLongKeysAreRejected() {
        IdempotencyRecordRepository repository = org.mockito.Mockito.mock(IdempotencyRecordRepository.class);
        IdempotencyService service = new IdempotencyService(repository);
        AccountEntity account = new AccountEntity("ACC-1", "Owner");

        assertNull(service.findExisting(account, IdempotencyOperationType.DEPOSIT, null,
                new BigDecimal("1.00"), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.findExisting(account, IdempotencyOperationType.DEPOSIT, "  ",
                        new BigDecimal("1.00"), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.findExisting(account, IdempotencyOperationType.DEPOSIT, "x".repeat(101),
                        new BigDecimal("1.00"), null));
    }

    @Test
    void identityIsScopedByAccountOperationAndKeyAndMatchingUsesBigDecimalCompareTo() {
        IdempotencyRecordRepository repository = org.mockito.Mockito.mock(IdempotencyRecordRepository.class);
        IdempotencyService service = new IdempotencyService(repository);
        AccountEntity first = new AccountEntity("ACC-1", "Owner");
        AccountEntity second = new AccountEntity("ACC-2", "Owner");
        JournalEntity journal = new JournalEntity("DEPOSIT-IDEMPOTENCY");
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(first, IdempotencyOperationType.DEPOSIT,
                "K", new BigDecimal("100.00"), null, new BigDecimal("200.00"), journal);
        when(repository.findByAccountAndIdempotencyKey(first, "K"))
                .thenReturn(Optional.of(record));
        when(repository.findByAccountAndIdempotencyKey(second, "K"))
                .thenReturn(Optional.empty());

        assertEquals(record, service.findExisting(first, IdempotencyOperationType.DEPOSIT, " K ",
                new BigDecimal("100.0"), null));
        assertNull(service.findExisting(second, IdempotencyOperationType.DEPOSIT, "K",
                new BigDecimal("100.00"), null));
        assertThrows(IdempotencyConflictException.class,
                () -> service.findExisting(first, IdempotencyOperationType.DEPOSIT, "K",
                        new BigDecimal("101.00"), null));
        verify(repository, org.mockito.Mockito.times(2)).findByAccountAndIdempotencyKey(first, "K");
    }

    @Test
    void transferMatchingIncludesRecipientAccountNumber() {
        IdempotencyRecordRepository repository = org.mockito.Mockito.mock(IdempotencyRecordRepository.class);
        IdempotencyService service = new IdempotencyService(repository);
        AccountEntity account = new AccountEntity("ACC-1", "Owner");
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(account, IdempotencyOperationType.TRANSFER,
                "K", new BigDecimal("10.00"), "ACC-2", new BigDecimal("90.00"),
                new JournalEntity("TRANSFER-IDEMPOTENCY"));
        when(repository.findByAccountAndIdempotencyKey(account, "K"))
                .thenReturn(Optional.of(record));

        assertEquals(record, service.findExisting(account, IdempotencyOperationType.TRANSFER, "K",
                new BigDecimal("10.00"), "ACC-2"));
        assertThrows(IdempotencyConflictException.class,
                () -> service.findExisting(account, IdempotencyOperationType.TRANSFER, "K",
                new BigDecimal("10.00"), "ACC-3"));
    }

    @Test
    void oneAccountKeyCannotChangeOperationType() {
        IdempotencyRecordRepository repository = org.mockito.Mockito.mock(IdempotencyRecordRepository.class);
        IdempotencyService service = new IdempotencyService(repository);
        AccountEntity account = new AccountEntity("ACC-1", "Owner");
        IdempotencyRecordEntity record = new IdempotencyRecordEntity(account, IdempotencyOperationType.DEPOSIT,
                "K", new BigDecimal("100.00"), null, new BigDecimal("200.00"),
                new JournalEntity("OPERATION-SCOPE"));
        when(repository.findByAccountAndIdempotencyKey(account, "K")).thenReturn(Optional.of(record));

        assertThrows(IdempotencyConflictException.class,
                () -> service.findExisting(account, IdempotencyOperationType.WITHDRAW, "K",
                        new BigDecimal("100.00"), null));
        assertThrows(IdempotencyConflictException.class,
                () -> service.findExisting(account, IdempotencyOperationType.TRANSFER, "K",
                        new BigDecimal("100.00"), "ACC-2"));
    }

    @Test
    void recordUniqueConstraintUsesAccountAndKeyOnly() {
        Table table = IdempotencyRecordEntity.class.getAnnotation(Table.class);
        assertEquals("uk_idempotency_account_key", table.uniqueConstraints()[0].name());
        assertArrayEquals(new String[] { "account_id", "idempotency_key" },
                table.uniqueConstraints()[0].columnNames());
    }
}
