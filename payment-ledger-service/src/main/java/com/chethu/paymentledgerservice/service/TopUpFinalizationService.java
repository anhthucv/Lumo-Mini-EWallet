package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.entity.TransactionEntity;
import com.chethu.paymentledgerservice.exception.InvalidPaymentWebhookException;
import com.chethu.paymentledgerservice.payment.provider.VerifiedPaymentWebhook;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;

@Service
public class TopUpFinalizationService {
    private static final Logger log = LoggerFactory.getLogger(TopUpFinalizationService.class);
    private static final String PROVIDER_CLEARING_CODE = "PROVIDER_CLEARING";

    private final TopUpPaymentRepository topUpPaymentRepository;
    private final AccountRepository accountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalRepository journalRepository;
    private final TransactionService transactionService;
    private final NotificationEventService notificationEventService;

    public TopUpFinalizationService(TopUpPaymentRepository topUpPaymentRepository,
            AccountRepository accountRepository, LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository, TransactionService transactionService,
            NotificationEventService notificationEventService) {
        this.topUpPaymentRepository = topUpPaymentRepository;
        this.accountRepository = accountRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalRepository = journalRepository;
        this.transactionService = transactionService;
        this.notificationEventService = notificationEventService;
    }

    @Transactional
    public void finalizeVerifiedWebhook(VerifiedPaymentWebhook webhook) {
        if (webhook == null || !webhook.successful()) {
            return;
        }
        if (webhook.merchantOrderCode() == null || webhook.merchantOrderCode() <= 0) {
            throw new InvalidPaymentWebhookException("Payment webhook order code is invalid.");
        }

        TopUpPaymentEntity payment = topUpPaymentRepository
                .findByMerchantOrderCodeForUpdate(webhook.merchantOrderCode()).orElse(null);
        if (payment == null) {
            log.warn("Ignoring verified webhook for unknown merchant order code: {}", webhook.merchantOrderCode());
            return;
        }
        if (payment.getStatus() == TopUpPaymentStatus.SUCCESS) {
            return;
        }
        validateFinancialData(payment, webhook);

        AccountEntity account = accountRepository.findByIdForUpdate(payment.getAccount().getId())
                .orElseThrow(() -> new InvalidPaymentWebhookException("Top-up wallet could not be found."));
        LedgerAccountEntity walletLedger = resolveWalletLedgerAccount(account);
        LedgerAccountEntity providerClearing = resolveProviderClearingAccount();

        account.deposit(payment.getAmount());
        JournalEntity journal = new JournalEntity("TOPUP-" + payment.getMerchantOrderCode());
        new LedgerEntryEntity(journal, providerClearing, LedgerEntryType.DEBIT, payment.getAmount());
        new LedgerEntryEntity(journal, walletLedger, LedgerEntryType.CREDIT, payment.getAmount());
        if (!journal.isBalanced()) {
            throw new IllegalStateException("Top-up ledger journal is not balanced");
        }
        journalRepository.save(journal);
        accountRepository.save(account);
        TransactionEntity transaction = transactionService.recordTransactionEntity(
                account, null, TransactionType.DEPOSIT, payment.getAmount(), account.getBalance(), journal);
        payment.markSuccessful(transaction, journal);
        topUpPaymentRepository.save(payment);
        notificationEventService.publishDepositSuccess(account, payment.getAmount(), journal.getReference());
    }

    private void validateFinancialData(TopUpPaymentEntity payment, VerifiedPaymentWebhook webhook) {
        if (webhook.provider() == null || payment.getProvider() != null
                && payment.getProvider() != webhook.provider()) {
            throw new InvalidPaymentWebhookException("Payment webhook provider does not match top-up payment.");
        }
        if (webhook.amount() == null || payment.getAmount().compareTo(webhook.amount()) != 0) {
            throw new InvalidPaymentWebhookException("Payment webhook amount does not match top-up payment.");
        }
        if (isBlank(webhook.currency()) || !payment.getCurrency().equalsIgnoreCase(webhook.currency())
                || !"VND".equalsIgnoreCase(webhook.currency())) {
            throw new InvalidPaymentWebhookException("Payment webhook currency does not match top-up payment.");
        }
        if (payment.getProviderReference() != null
                && !payment.getProviderReference().equals(webhook.providerReference())) {
            throw new InvalidPaymentWebhookException("Payment webhook reference does not match top-up payment.");
        }
    }

    private LedgerAccountEntity resolveWalletLedgerAccount(AccountEntity account) {
        return ledgerAccountRepository.findByWalletAccount(account)
                .orElseGet(() -> ledgerAccountRepository.save(new LedgerAccountEntity(
                        "WALLET-" + account.getAccountNumber(), LedgerAccountType.WALLET,
                        AccountClass.LIABILITY, account)));
    }

    private LedgerAccountEntity resolveProviderClearingAccount() {
        return ledgerAccountRepository.findByCode(PROVIDER_CLEARING_CODE)
                .orElseGet(() -> ledgerAccountRepository.save(new LedgerAccountEntity(
                        PROVIDER_CLEARING_CODE, LedgerAccountType.PROVIDER_CLEARING,
                        AccountClass.ASSET, null)));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
