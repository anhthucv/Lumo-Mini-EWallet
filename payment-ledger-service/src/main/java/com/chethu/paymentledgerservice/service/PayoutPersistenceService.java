package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.WalletRules;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;
import com.chethu.paymentledgerservice.entity.PayoutEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.AccountNotActiveException;
import com.chethu.paymentledgerservice.exception.IdempotencyConflictException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.PayoutRepository;

@Service
public class PayoutPersistenceService {
    private final AccountRepository accountRepository;
    private final PayoutRepository payoutRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalRepository journalRepository;
    private final TransactionLimitService transactionLimitService;
    private final RiskEvaluationService riskEvaluationService;
    private final RiskAuditService riskAuditService;

    public PayoutPersistenceService(AccountRepository accountRepository, PayoutRepository payoutRepository,
            LedgerAccountRepository ledgerAccountRepository, JournalRepository journalRepository,
            TransactionLimitService transactionLimitService, RiskEvaluationService riskEvaluationService,
            RiskAuditService riskAuditService) {
        this.accountRepository = accountRepository;
        this.payoutRepository = payoutRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalRepository = journalRepository;
        this.transactionLimitService = transactionLimitService;
        this.riskEvaluationService = riskEvaluationService;
        this.riskAuditService = riskAuditService;
    }

    @Transactional
    public PayoutEntity reserve(Long userId, BigDecimal amount, String bankIdentifier,
            String accountSummary, String accountHash, String accountEncrypted, String idempotencyKey) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        account = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new AccountNotFoundException(userId));
        final AccountEntity lockedAccount = account;
        PayoutEntity existing = payoutRepository.findByAccountAndIdempotencyKeyForUpdate(account, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (existing.getAmount().compareTo(amount) != 0
                    || !existing.getDestinationBankIdentifier().equals(bankIdentifier)
                    || !existing.getDestinationAccountHash().equals(accountHash)) {
                throw new IdempotencyConflictException();
            }
            return existing;
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        transactionLimitService.validate(account, LimitOperationType.WITHDRAW, amount);
        RiskEvaluationResult risk = riskEvaluationService.evaluate(account, LimitOperationType.WITHDRAW, amount);
        if (risk.decision() == RiskDecision.REJECT) {
            riskAuditService.recordRejected(account, LimitOperationType.WITHDRAW, amount, risk);
            throw new com.chethu.paymentledgerservice.exception.RiskRejectedException();
        }
        if (risk.decision() == RiskDecision.FLAG) {
            riskAuditService.recordFlagged(account, LimitOperationType.WITHDRAW, amount, risk);
        }

        account.withdraw(amount, WalletRules.MINIMUM_BALANCE);
        LedgerAccountEntity wallet = ledgerAccountRepository.findByWalletAccount(lockedAccount).orElseGet(
                () -> ledgerAccountRepository.save(new LedgerAccountEntity("WALLET-" + lockedAccount.getAccountNumber(),
                        LedgerAccountType.WALLET, AccountClass.LIABILITY, lockedAccount)));
        LedgerAccountEntity pending = ledgerAccountRepository.findByCode("PAYOUT_PENDING").orElseGet(
                () -> ledgerAccountRepository.save(new LedgerAccountEntity("PAYOUT_PENDING",
                        LedgerAccountType.PAYOUT_PENDING, AccountClass.LIABILITY, null)));
        JournalEntity journal = new JournalEntity("PAYOUT-RESERVE-" + UUID.randomUUID());
        new LedgerEntryEntity(journal, wallet, LedgerEntryType.DEBIT, amount);
        new LedgerEntryEntity(journal, pending, LedgerEntryType.CREDIT, amount);
        if (!journal.isBalanced()) {
            throw new IllegalStateException("Payout reservation journal is not balanced");
        }
        journalRepository.save(journal);
        accountRepository.save(lockedAccount);
        return payoutRepository.save(new PayoutEntity(lockedAccount, amount, "PAYOUT-" + UUID.randomUUID(),
                bankIdentifier, accountSummary, accountHash, accountEncrypted, idempotencyKey, journal));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PayoutEntity markProviderRequestStarted(Long payoutId) {
        PayoutEntity payout = payoutRepository.findById(payoutId).orElseThrow();
        payout.markProviderRequestStarted();
        return payoutRepository.save(payout);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PayoutEntity attachProviderReference(Long payoutId, String providerReference) {
        PayoutEntity payout = payoutRepository.findById(payoutId).orElseThrow();
        payout.attachProviderReference(providerReference);
        return payoutRepository.save(payout);
    }
}
