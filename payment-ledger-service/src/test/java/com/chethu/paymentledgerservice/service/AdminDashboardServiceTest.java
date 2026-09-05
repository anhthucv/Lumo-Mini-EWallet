package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

class AdminDashboardServiceTest {
    @Test
    void dashboard_shouldUseCountsMeaningfulVolumesAndFiveRecentRows() {
        UserRepository users = Mockito.mock(UserRepository.class);
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TransactionRepository transactions = Mockito.mock(TransactionRepository.class);
        when(users.count()).thenReturn(4L);
        when(users.countByStatus(UserStatus.ACTIVE)).thenReturn(3L);
        when(users.countByStatus(UserStatus.LOCKED)).thenReturn(1L);
        when(accounts.count()).thenReturn(4L);
        when(transactions.count()).thenReturn(10L);
        when(transactions.countByStatus(TransactionStatus.SUCCESS)).thenReturn(8L);
        when(transactions.sumByTypeAndStatus(TransactionType.DEPOSIT, TransactionStatus.SUCCESS)).thenReturn(new BigDecimal("100"));
        when(transactions.sumByTypeAndStatus(TransactionType.TRANSFER_OUT, TransactionStatus.SUCCESS)).thenReturn(new BigDecimal("200"));
        when(transactions.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of()));

        var result = new AdminDashboardService(users, accounts, transactions).getDashboard();

        assertEquals(4, result.users().total());
        assertEquals(3, result.users().active());
        assertEquals(1, result.users().locked());
        assertEquals(new BigDecimal("100"), result.transactions().depositVolume());
        assertEquals(new BigDecimal("200"), result.transactions().transferVolume());
    }
}
