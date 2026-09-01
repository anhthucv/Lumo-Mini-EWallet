package com.chethu.paymentledgerservice.service;

import java.util.ArrayList;
import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.domain.WalletRules;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.CreateAccountRequest;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.MyWalletResponse;
import com.chethu.paymentledgerservice.dto.RecipientResponse;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.dto.UpdateAccountRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.InvalidAccountNumberException;
import com.chethu.paymentledgerservice.exception.InvalidTransferException;
import com.chethu.paymentledgerservice.repository.AccountRepository;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(AccountRepository accountRepository,TransactionService transactionService,
            AccountNumberGenerator accountNumberGenerator){
        this.accountRepository=accountRepository;
        this.transactionService = transactionService;
        this.accountNumberGenerator = accountNumberGenerator;
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
    public AccountResponse depositForCurrentUser(Long userId, MoneyOperationRequest request) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        return depositToAccount(account, request);
    }

    private AccountResponse depositToAccount(AccountEntity account, MoneyOperationRequest request) {
        if (request == null || request.getAmount() == null
                || request.getAmount().compareTo(WalletRules.MINIMUM_OPERATION_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least 1,000 VNĐ");
        }
        account.deposit(request.getAmount());
        AccountEntity updatedAccount = accountRepository.save(account);
        transactionService.recordTransaction(account, null, TransactionType.DEPOSIT, request.getAmount(),account.getBalance());
        return toResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse withdrawForCurrentUser(Long userId, MoneyOperationRequest request){
        AccountEntity account = accountRepository.findByUserId(userId)
        .orElseThrow(()->new AccountNotFoundException(userId));
        return withdrawFromAccount(account,request);
    }

    private AccountResponse withdrawFromAccount(AccountEntity account, MoneyOperationRequest request){
        if (request == null || request.getAmount() == null
                || request.getAmount().compareTo(WalletRules.MINIMUM_OPERATION_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be at least 1,000 VNĐ");
        }
        account.withdraw(request.getAmount(), WalletRules.MINIMUM_BALANCE);
        AccountEntity updatedAccount = accountRepository.save(account);
        transactionService.recordTransaction(account, null, TransactionType.WITHDRAW, request.getAmount(), account.getBalance());
        return toResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse transferForCurrentUser(Long userId, TransferRequest request) {
        validateTransferRequest(request);
        AccountEntity sender = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        AccountEntity recipient = accountRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getRecipientAccountNumber()));
        return transferBetweenAccounts(sender, recipient, request);
    }

    private AccountResponse transferBetweenAccounts(AccountEntity sender, AccountEntity recipient,
            TransferRequest request) {
        if (sender == recipient || (sender.getId() != null && sender.getId().equals(recipient.getId()))) {
            throw new InvalidTransferException("Sender and receiver account must be different");
        }
        sender.withdraw(request.getAmount(), WalletRules.MINIMUM_BALANCE);
        recipient.deposit(request.getAmount());
        AccountEntity updatedSender = accountRepository.save(sender);
        accountRepository.save(recipient);

        transactionService.recordTransaction(sender, recipient, TransactionType.TRANSFER_OUT,
                request.getAmount(), sender.getBalance());
        transactionService.recordTransaction(recipient, sender, TransactionType.TRANSFER_IN,
                request.getAmount(), recipient.getBalance());
        return toResponse(updatedSender);
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
