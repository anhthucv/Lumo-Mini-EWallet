package com.chethu.paymentledgerservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.WalletRules;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.dto.TransferResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.InvalidTransferException;
import com.chethu.paymentledgerservice.repository.AccountRepository;

@Service
public class TransferService {
    

    private final AccountRepository accountRepository;
    public TransferService(AccountRepository accountRepository){
        this.accountRepository = accountRepository;

    }

    private AccountEntity findAccountById(Long id){
        return accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }


    @Transactional
    public TransferResponse transfer (TransferRequest request){
        if (request.getFromAccountId().equals(request.getToAccountId()))
            throw new InvalidTransferException("Sender and receiver account must be different");
        AccountEntity fromAccount = findAccountById(request.getFromAccountId());
        AccountEntity toAccount = findAccountById(request.getToAccountId());
        fromAccount.withdraw(request.getAmount(), WalletRules.MINIMUM_BALANCE);
        toAccount.deposit(request.getAmount());
        AccountEntity updatedFromAccount = accountRepository.save(fromAccount);
        AccountEntity updatedToAccount = accountRepository.save(toAccount);
        return new TransferResponse(
            updatedFromAccount.getId(), 
            updatedToAccount.getId(),
            request.getAmount(), 
            updatedFromAccount.getBalance(),
            updatedToAccount.getBalance(),
            "SUCCESS"
        );
        
    }

}
