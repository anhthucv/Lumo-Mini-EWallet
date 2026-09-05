package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AdminTransactionNotFoundException;
import com.chethu.paymentledgerservice.repository.LedgerEntryRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

class AdminTransactionServiceTest {
    @Test
    void list_shouldMapPageWithoutLoadingLedgerEntries() {
        TransactionRepository transactions = Mockito.mock(TransactionRepository.class);
        LedgerEntryRepository entries = Mockito.mock(LedgerEntryRepository.class);
        TopUpPaymentRepository topUps = Mockito.mock(TopUpPaymentRepository.class);
        when(transactions.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.<TransactionEntity>of()));

        var result = new AdminTransactionService(transactions, entries, topUps)
                .list(null, null, null, null, null, PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
        Mockito.verifyNoInteractions(entries, topUps);
    }

    @Test
    void detail_shouldReturnNotFoundForUnknownTransaction() {
        TransactionRepository transactions = Mockito.mock(TransactionRepository.class);
        when(transactions.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThrows(AdminTransactionNotFoundException.class,
                () -> new AdminTransactionService(transactions, Mockito.mock(LedgerEntryRepository.class),
                        Mockito.mock(TopUpPaymentRepository.class)).detail(99L));
    }
}
