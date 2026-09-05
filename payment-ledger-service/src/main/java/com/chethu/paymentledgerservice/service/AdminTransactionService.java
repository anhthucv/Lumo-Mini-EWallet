package com.chethu.paymentledgerservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.AdminLedgerEntryResponse;
import com.chethu.paymentledgerservice.dto.AdminTransactionDetailResponse;
import com.chethu.paymentledgerservice.dto.AdminTransactionResponse;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.AdminTransactionNotFoundException;
import com.chethu.paymentledgerservice.repository.LedgerEntryRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;

@Service
public class AdminTransactionService {
    private final TransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final TopUpPaymentRepository topUps;

    public AdminTransactionService(TransactionRepository transactions, LedgerEntryRepository entries,
            TopUpPaymentRepository topUps) {
        this.transactions = transactions;
        this.entries = entries;
        this.topUps = topUps;
    }

    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> list(String search, TransactionType type, TransactionStatus status,
            LocalDate from, LocalDate to, Pageable pageable) {
        validateDates(from, to);
        Specification<TransactionEntity> spec = (root, query, cb) -> cb.conjunction();
        if (type != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("transactionType"), type));
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (from != null) { LocalDateTime value = from.atStartOfDay(); spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), value)); }
        if (to != null) { LocalDateTime value = to.plusDays(1).atStartOfDay(); spec = spec.and((r, q, cb) -> cb.lessThan(r.get("createdAt"), value)); }
        if (search != null && !search.isBlank()) {
            String value = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> cb.or(
                    cb.like(cb.lower(r.join("account").join("user").get("email")), value),
                    cb.like(cb.lower(r.join("account").join("user").get("fullName")), value),
                    cb.like(cb.lower(r.join("account").get("accountNumber")), value)));
        }
        return transactions.findAll(spec, pageable).map(AdminTransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminTransactionDetailResponse detail(Long id) {
        TransactionEntity tx = transactions.findById(id).orElseThrow(() -> new AdminTransactionNotFoundException(id));
        var journal = tx.getJournal();
        var account = tx.getAccount();
        var user = account.getUser();
        var ledger = journal == null ? java.util.List.<AdminLedgerEntryResponse>of()
                : entries.findByJournalOrderByIdAsc(journal).stream().map(AdminLedgerEntryResponse::from).toList();
        var topUp = topUps.findByTransactionId(id)
                .map(p -> new AdminTransactionDetailResponse.AdminTopUpSummary(p.getId(), p.getProvider() == null ? null : p.getProvider().name(),
                        p.getStatus() == null ? null : p.getStatus().name(), p.getProviderReference(), p.getMerchantOrderCode(), p.getAmount())).orElse(null);
        return new AdminTransactionDetailResponse(tx.getId(), tx.getTransactionType(), tx.getStatus(), tx.getAmount(),
                tx.getBalanceAfterTransaction(), tx.getCreatedAt(), user == null ? null : user.getId(), user == null ? null : user.getEmail(),
                user == null ? null : user.getFullName(), account.getId(), AdminTransactionResponse.mask(account.getAccountNumber()),
                tx.getRelatedAccount() == null ? null : tx.getRelatedAccount().getId(), journal == null ? null : journal.getId(),
                journal == null ? null : journal.getReference(), journal == null ? null : journal.getCreatedAt(), ledger, topUp);
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("from must be on or before to");
    }
}
