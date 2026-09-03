package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.chethu.paymentledgerservice.PaymentLedgerServiceApplication;
import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.LedgerEntryRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;

/**
 * Requires an explicitly configured local MySQL database named
 * payment_ledger_test. It is skipped unless all TEST_DB_* variables are safe.
 */
@SpringBootTest(classes = PaymentLedgerServiceApplication.class)
@Import(TransactionRollbackIntegrationTest.RollbackTestConfiguration.class)
@ActiveProfiles("rollback-integration")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL",
        matches = "jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?/payment_ledger_test(?:\\?.*)?")
@EnabledIfEnvironmentVariable(named = "TEST_DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TEST_DB_PASSWORD", matches = ".+")
class TransactionRollbackIntegrationTest {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JournalRepository journalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AccountService accountService;
    private final FailingTransactionService failingTransactionService;
    private final FailingIdempotencyService failingIdempotencyService;

    TransactionRollbackIntegrationTest(AccountRepository accountRepository, UserRepository userRepository,
            JournalRepository journalRepository, LedgerEntryRepository ledgerEntryRepository,
            LedgerAccountRepository ledgerAccountRepository, TransactionRepository transactionRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            AccountService accountService,
            FailingTransactionService failingTransactionService,
            FailingIdempotencyService failingIdempotencyService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.accountService = accountService;
        this.failingTransactionService = failingTransactionService;
        this.failingIdempotencyService = failingIdempotencyService;
    }

    @DynamicPropertySource
    static void localDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> required("TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> required("TEST_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> required("TEST_DB_PASSWORD"));
    }

    @BeforeEach
    void cleanDedicatedDatabase() {
        failingTransactionService.disableFailure();
        failingIdempotencyService.disableFailure();
        idempotencyRecordRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        ledgerEntryRepository.deleteAllInBatch();
        journalRepository.deleteAllInBatch();
        ledgerAccountRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void depositRollsBackBalanceLedgerTransactionAndIdempotency() {
        WalletFixture wallet = wallet("deposit-user", "DEP-1", "100.00");
        Snapshot before = snapshot();
        failingTransactionService.failAfterCall(1);

        assertThrows(IllegalStateException.class,
                () -> accountService.depositForCurrentUser(wallet.user().getId(), money("25.00"), "dep-key"));

        assertRolledBack(before, wallet.account().getId(), new BigDecimal("100.00"));
    }

    @Test
    void withdrawRollsBackBalanceLedgerTransactionAndIdempotency() {
        WalletFixture wallet = wallet("withdraw-user", "WDR-1", "200000.00");
        Snapshot before = snapshot();
        failingTransactionService.failAfterCall(1);

        assertThrows(IllegalStateException.class,
                () -> accountService.withdrawForCurrentUser(wallet.user().getId(), money("25000.00"), "wdr-key"));

        assertRolledBack(before, wallet.account().getId(), new BigDecimal("200000.00"));
    }

    @Test
    void transferRollsBackBothBalancesAndBothTransactionsWhenSecondTransactionFails() {
        WalletFixture sender = wallet("sender", "TRF-S", "200000.00");
        WalletFixture recipient = wallet("recipient", "TRF-R", "100000.00");
        Snapshot before = snapshot();
        failingTransactionService.failAfterCall(2);

        assertThrows(IllegalStateException.class, () -> accountService.transferForCurrentUser(
                sender.user().getId(), transfer(recipient.account().getAccountNumber(), "25000.00"), "trf-key"));

        assertRolledBack(before, sender.account().getId(), new BigDecimal("200000.00"));
        assertEquals(new BigDecimal("100000.00"), accountRepository.findById(recipient.account().getId())
                .orElseThrow().getBalance());
    }

    @Test
    void idempotencyCompletionFailureRollsBackFinancialPosting() {
        WalletFixture wallet = wallet("idempotency-user", "IDP-1", "100.00");
        Snapshot before = snapshot();
        failingIdempotencyService.enableFailure();

        assertThrows(IllegalStateException.class,
                () -> accountService.depositForCurrentUser(wallet.user().getId(), money("25.00"), "idp-key"));

        assertRolledBack(before, wallet.account().getId(), new BigDecimal("100.00"));
    }

    private void assertRolledBack(Snapshot before, Long accountId, BigDecimal expectedBalance) {
        assertEquals(expectedBalance, accountRepository.findById(accountId).orElseThrow().getBalance());
        assertEquals(before.accounts(), accountRepository.count());
        assertEquals(before.journals(), journalRepository.count());
        assertEquals(before.ledgerEntries(), ledgerEntryRepository.count());
        assertEquals(before.transactions(), transactionRepository.count());
        assertEquals(before.idempotencyRecords(), idempotencyRecordRepository.count());
    }

    private Snapshot snapshot() {
        return new Snapshot(accountRepository.count(), journalRepository.count(), ledgerEntryRepository.count(),
                transactionRepository.count(), idempotencyRecordRepository.count());
    }

    private WalletFixture wallet(String email, String accountNumber, String balance) {
        UserEntity user = userRepository.save(new UserEntity(email + "@test.local", "hash", "Test User",
                UserRole.USER, UserStatus.ACTIVE));
        AccountEntity account = new AccountEntity(accountNumber, "Test User");
        account.assignUser(user);
        account.deposit(new BigDecimal(balance));
        account = accountRepository.save(account);
        ledgerAccountRepository.save(new LedgerAccountEntity("WALLET-" + accountNumber, LedgerAccountType.WALLET,
                AccountClass.LIABILITY, account));
        ledgerAccountRepository.save(new LedgerAccountEntity("SYSTEM_CLEARING-" + accountNumber,
                LedgerAccountType.SYSTEM_CLEARING, AccountClass.ASSET, null));
        return new WalletFixture(user, account);
    }

    private MoneyOperationRequest money(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private TransferRequest transfer(String recipient, String amount) {
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber(recipient);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for rollback integration tests");
        }
        return value;
    }

    private record Snapshot(long accounts, long journals, long ledgerEntries, long transactions,
            long idempotencyRecords) {
    }

    private record WalletFixture(UserEntity user, AccountEntity account) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RollbackTestConfiguration {
        @Bean
        @Primary
        FailingTransactionService failingTransactionService(TransactionRepository transactionRepository,
                AccountRepository accountRepository) {
            return new FailingTransactionService(transactionRepository, accountRepository);
        }

        @Bean
        @Primary
        FailingIdempotencyService failingIdempotencyService(IdempotencyRecordRepository repository) {
            return new FailingIdempotencyService(repository);
        }
    }

    static class FailingTransactionService extends TransactionService {
        private int failAfterCall;
        private int callCount;

        FailingTransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
            super(transactionRepository, accountRepository);
        }

        void failAfterCall(int call) {
            this.callCount = 0;
            this.failAfterCall = call;
        }

        void disableFailure() {
            this.callCount = 0;
            this.failAfterCall = 0;
        }

        @Override
        public void recordTransaction(com.chethu.paymentledgerservice.entity.AccountEntity account,
                com.chethu.paymentledgerservice.entity.AccountEntity relatedAccount, TransactionType type,
                BigDecimal amount, BigDecimal balance,
                com.chethu.paymentledgerservice.entity.JournalEntity journal) {
            super.recordTransaction(account, relatedAccount, type, amount, balance, journal);
            callCount++;
            if (callCount == failAfterCall) {
                throw new IllegalStateException("Injected transaction persistence failure");
            }
        }
    }

    static class FailingIdempotencyService extends IdempotencyService {
        private boolean fail;

        FailingIdempotencyService(IdempotencyRecordRepository repository) {
            super(repository);
        }

        void enableFailure() {
            fail = true;
        }

        void disableFailure() {
            fail = false;
        }

        @Override
        public void saveCompleted(com.chethu.paymentledgerservice.entity.AccountEntity account,
                IdempotencyOperationType operationType, String key, BigDecimal amount, String recipient,
                BigDecimal resultBalance, com.chethu.paymentledgerservice.entity.JournalEntity journal) {
            super.saveCompleted(account, operationType, key, amount, recipient, resultBalance, journal);
            if (fail) {
                throw new IllegalStateException("Injected idempotency completion failure");
            }
        }
    }
}
