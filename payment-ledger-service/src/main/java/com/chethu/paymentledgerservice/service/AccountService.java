package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.domain.LimitOperationType;
import com.chethu.paymentledgerservice.domain.IdempotencyOperationType;
import com.chethu.paymentledgerservice.domain.WalletRules;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.CreateAccountRequest;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.MyWalletResponse;
import com.chethu.paymentledgerservice.dto.RecipientResponse;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.dto.UpdateAccountRequest;
import com.chethu.paymentledgerservice.dto.WalletLimitsResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.AccountNotActiveException;
import com.chethu.paymentledgerservice.exception.InvalidAccountNumberException;
import com.chethu.paymentledgerservice.exception.InvalidTransferException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalRepository journalRepository;
    private final IdempotencyService idempotencyService;
    private final TransactionLimitService transactionLimitService;

    public AccountService(AccountRepository accountRepository,TransactionService transactionService,
            AccountNumberGenerator accountNumberGenerator, LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository, IdempotencyService idempotencyService,
            TransactionLimitService transactionLimitService){
        this.accountRepository=accountRepository;
        this.transactionService = transactionService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalRepository = journalRepository;
        this.idempotencyService = idempotencyService;
        this.transactionLimitService = transactionLimitService;
    }


    private AccountResponse toResponse(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance(),
                account.getStatus()
        );
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        String accountNumber= accountNumberGenerator.generateUniqueAccountNumber();
        AccountEntity account = new AccountEntity(accountNumber,request.getOwnerName());
        AccountEntity savedAccount = accountRepository.save(account);
        return toResponse(savedAccount);
    }

    private AccountEntity findAccountById(Long id){
        return accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public AccountResponse getAccountById(Long id){
        AccountEntity account = findAccountById(id);
        return toResponse(account);
    }

    public MyWalletResponse getMyWallet(Long userId) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        return MyWalletResponse.from(account);
    }

    public WalletLimitsResponse getWalletLimits(Long userId) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        return transactionLimitService.getWalletLimits(account);
    }

    public RecipientResponse getRecipient(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidAccountNumberException();
        }

        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return RecipientResponse.from(account);
    }

    public List<AccountResponse> getAllAccounts(){
        List<AccountEntity> entities = accountRepository.findAll();
        List<AccountResponse> responses = new ArrayList<>();
        for (AccountEntity entity: entities){
            AccountResponse response = toResponse(entity);
            responses.add(response);
        }
        return responses;
    }

    public void deleteAccount(Long id){
        AccountEntity account = findAccountById(id);
        accountRepository.delete(account);

    }

    public AccountResponse updateAccount(Long id, UpdateAccountRequest request){
        AccountEntity account = findAccountById(id);
        account.changeOwnerName(request.getOwnerName());
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse depositForCurrentUser(Long userId, MoneyOperationRequest request, String idempotencyKey) {
        validateMoneyRequest(request);
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        account = lockAccount(account);
        IdempotencyRecordEntity existing = idempotencyService.findExisting(account,
                IdempotencyOperationType.DEPOSIT, idempotencyKey, request.getAmount(), null);
        if (existing != null) {
            return replayResponse(account, existing);
        }
        ensureAccountActive(account);
        validateLimit(account, LimitOperationType.DEPOSIT, request.getAmount());
        PostedOperation posted = depositToAccount(account, request);
        idempotencyService.saveCompleted(account, IdempotencyOperationType.DEPOSIT, idempotencyKey,
                request.getAmount(), null, account.getBalance(), posted.journal());
        return posted.response();
    }

    private PostedOperation depositToAccount(AccountEntity account, MoneyOperationRequest request) {
        LedgerAccountEntity walletLedgerAccount = resolveWalletLedgerAccount(account);
        LedgerAccountEntity systemClearingAccount = resolveSystemClearingAccount();
        account.deposit(request.getAmount());
        JournalEntity journal = new JournalEntity("DEPOSIT-" + UUID.randomUUID());
        new LedgerEntryEntity(journal, systemClearingAccount, LedgerEntryType.DEBIT, request.getAmount());
        new LedgerEntryEntity(journal, walletLedgerAccount, LedgerEntryType.CREDIT, request.getAmount());
        if (!journal.isBalanced()) {
            throw new IllegalStateException("Deposit ledger journal is not balanced");
        }
        journalRepository.save(journal);
        AccountEntity updatedAccount = accountRepository.save(account);
        transactionService.recordTransaction(account, null, TransactionType.DEPOSIT, request.getAmount(),
                account.getBalance(), journal);
        return new PostedOperation(toResponse(updatedAccount), journal);
    }

    private LedgerAccountEntity resolveWalletLedgerAccount(AccountEntity account) {
        return ledgerAccountRepository.findByWalletAccount(account)
                .orElseGet(() -> ledgerAccountRepository.save(new LedgerAccountEntity(
                        "WALLET-" + account.getAccountNumber(), LedgerAccountType.WALLET,
                        AccountClass.LIABILITY, account)));
    }

    private LedgerAccountEntity resolveSystemClearingAccount() {
        return ledgerAccountRepository.findByCode("SYSTEM_CLEARING")
                .orElseGet(() -> ledgerAccountRepository.save(new LedgerAccountEntity(
                        "SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                        AccountClass.ASSET, null)));
    }

    @Transactional
    public AccountResponse withdrawForCurrentUser(Long userId, MoneyOperationRequest request, String idempotencyKey){
        validateMoneyRequest(request);
        AccountEntity account = accountRepository.findByUserId(userId)
        .orElseThrow(()->new AccountNotFoundException(userId));
        account = lockAccount(account);
        IdempotencyRecordEntity existing = idempotencyService.findExisting(account,
                IdempotencyOperationType.WITHDRAW, idempotencyKey, request.getAmount(), null);
        if (existing != null) {
            return replayResponse(account, existing);
        }
        ensureAccountActive(account);
        validateLimit(account, LimitOperationType.WITHDRAW, request.getAmount());
        PostedOperation posted = withdrawFromAccount(account,request);
        idempotencyService.saveCompleted(account, IdempotencyOperationType.WITHDRAW, idempotencyKey,
                request.getAmount(), null, account.getBalance(), posted.journal());
        return posted.response();
    }

    private PostedOperation withdrawFromAccount(AccountEntity account, MoneyOperationRequest request){
        LedgerAccountEntity walletLedgerAccount = resolveWalletLedgerAccount(account);
        LedgerAccountEntity systemClearingAccount = resolveSystemClearingAccount();
        account.withdraw(request.getAmount(), WalletRules.MINIMUM_BALANCE);
        JournalEntity journal = new JournalEntity("WITHDRAW-" + UUID.randomUUID());
        new LedgerEntryEntity(journal, walletLedgerAccount, LedgerEntryType.DEBIT, request.getAmount());
        new LedgerEntryEntity(journal, systemClearingAccount, LedgerEntryType.CREDIT, request.getAmount());
        if (!journal.isBalanced()) {
            throw new IllegalStateException("Withdraw ledger journal is not balanced");
        }
        journalRepository.save(journal);
        AccountEntity updatedAccount = accountRepository.save(account);
        transactionService.recordTransaction(account, null, TransactionType.WITHDRAW, request.getAmount(),
                account.getBalance(), journal);
        return new PostedOperation(toResponse(updatedAccount), journal);
    }

    @Transactional
    public AccountResponse transferForCurrentUser(Long userId, TransferRequest request, String idempotencyKey) {
        validateTransferRequest(request);
        AccountEntity sender = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        AccountEntity recipient = accountRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getRecipientAccountNumber()));
        if (sender == recipient || (sender.getId() != null && sender.getId().equals(recipient.getId()))) {
            throw new InvalidTransferException("Sender and receiver account must be different");
        }
        AccountPair locked = lockAccounts(sender, recipient);
        sender = locked.sender();
        recipient = locked.recipient();
        IdempotencyRecordEntity existing = idempotencyService.findExisting(sender,
                IdempotencyOperationType.TRANSFER, idempotencyKey, request.getAmount(),
                request.getRecipientAccountNumber());
        if (existing != null) {
            return replayResponse(sender, existing);
        }
        ensureAccountActive(sender);
        ensureAccountActive(recipient);
        validateLimit(sender, LimitOperationType.TRANSFER, request.getAmount());
        PostedOperation posted = transferBetweenAccounts(sender, recipient, request);
        idempotencyService.saveCompleted(sender, IdempotencyOperationType.TRANSFER, idempotencyKey,
                request.getAmount(), request.getRecipientAccountNumber(), sender.getBalance(), posted.journal());
        return posted.response();
    }

    private void ensureAccountActive(AccountEntity account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
    }

    private void validateLimit(AccountEntity account, LimitOperationType operation, BigDecimal amount) {
        if (transactionLimitService != null) {
            transactionLimitService.validate(account, operation, amount);
        }
    }

    private AccountEntity lockAccount(AccountEntity account) {
        if (account.getId() == null) {
            throw new AccountNotFoundException(account.getAccountNumber());
        }
        return accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new AccountNotFoundException(account.getId()));
    }

    private AccountPair lockAccounts(AccountEntity first, AccountEntity second) {
        Long firstId = first.getId();
        Long secondId = second.getId();
        if (firstId == null || secondId == null) {
            throw new InvalidTransferException("Account identifiers are required for transfer");
        }
        Long lowerId = firstId.compareTo(secondId) < 0 ? firstId : secondId;
        Long higherId = firstId.compareTo(secondId) < 0 ? secondId : firstId;
        AccountEntity lower = accountRepository.findByIdForUpdate(lowerId)
                .orElseThrow(() -> new AccountNotFoundException(lowerId));
        AccountEntity higher = accountRepository.findByIdForUpdate(higherId)
                .orElseThrow(() -> new AccountNotFoundException(higherId));
        return firstId.equals(lowerId) ? new AccountPair(lower, higher) : new AccountPair(higher, lower);
    }

    private PostedOperation transferBetweenAccounts(AccountEntity sender, AccountEntity recipient,
            TransferRequest request) {
        if (sender == recipient || (sender.getId() != null && sender.getId().equals(recipient.getId()))) {
            throw new InvalidTransferException("Sender and receiver account must be different");
        }
        LedgerAccountEntity senderLedgerAccount = resolveWalletLedgerAccount(sender);
        LedgerAccountEntity recipientLedgerAccount = resolveWalletLedgerAccount(recipient);
        sender.withdraw(request.getAmount(), WalletRules.MINIMUM_BALANCE);
        recipient.deposit(request.getAmount());
        JournalEntity journal = new JournalEntity("TRANSFER-" + UUID.randomUUID());
        new LedgerEntryEntity(journal, senderLedgerAccount, LedgerEntryType.DEBIT, request.getAmount());
        new LedgerEntryEntity(journal, recipientLedgerAccount, LedgerEntryType.CREDIT, request.getAmount());
        if (!journal.isBalanced()) {
            throw new IllegalStateException("Transfer ledger journal is not balanced");
        }
        journalRepository.save(journal);
        AccountEntity updatedSender = accountRepository.save(sender);
        accountRepository.save(recipient);

        transactionService.recordTransaction(sender, recipient, TransactionType.TRANSFER_OUT,
                request.getAmount(), sender.getBalance(), journal);
        transactionService.recordTransaction(recipient, sender, TransactionType.TRANSFER_IN,
                request.getAmount(), recipient.getBalance(), journal);
        return new PostedOperation(toResponse(updatedSender), journal);
    }

    private void validateMoneyRequest(MoneyOperationRequest request) {
        if (request == null || request.getAmount() == null
                || request.getAmount().compareTo(WalletRules.MINIMUM_OPERATION_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least 1 VNĐ");
        }
    }

    private AccountResponse replayResponse(AccountEntity account, IdempotencyRecordEntity record) {
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getOwnerName(),
                record.getResultBalance(), account.getStatus());
    }

    private record PostedOperation(AccountResponse response, JournalEntity journal) {
    }

    private record AccountPair(AccountEntity sender, AccountEntity recipient) {
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request == null || request.getRecipientAccountNumber() == null
                || request.getRecipientAccountNumber().isBlank()) {
            throw new InvalidTransferException("Recipient account number must not be blank");
        }
        if (request.getAmount() == null
                || request.getAmount().compareTo(WalletRules.MINIMUM_OPERATION_AMOUNT) < 0) {
            throw new InvalidTransferException("Amount must be at least 1 VNĐ");
        }
    }

}
