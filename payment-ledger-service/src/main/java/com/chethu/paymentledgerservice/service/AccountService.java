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
import com.chethu.paymentledgerservice.dto.UpdateAccountRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
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
    public AccountResponse deposit(Long id, MoneyOperationRequest request){
        AccountEntity account = findAccountById(id);
        account.deposit(request.getAmount());
        AccountEntity updatedAccount = accountRepository.save(account);
        transactionService.recordTransaction(account, null, TransactionType.DEPOSIT, request.getAmount(),account.getBalance());
        return toResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse withdraw(Long id, MoneyOperationRequest request){
        AccountEntity account = findAccountById(id);
        account.withdraw(request.getAmount(), WalletRules.MINIMUM_BALANCE);
        AccountEntity updatedAccount = accountRepository.save(account);
        transactionService.recordTransaction(account, null, TransactionType.WITHDRAW, request.getAmount(), account.getBalance());
        return toResponse(updatedAccount);
    }

}
