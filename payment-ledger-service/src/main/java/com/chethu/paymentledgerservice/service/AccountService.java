package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.CreateAccountRequest;
import com.chethu.paymentledgerservice.dto.UpdateAccountRequest;

@Service
public class AccountService {
    public AccountResponse createAccount(CreateAccountRequest request) {
        String ownerName = request.getOwnerName();

        return new AccountResponse(
                1L,
                "ACC-001",
                ownerName,
                BigDecimal.ZERO.setScale(2),
                AccountStatus.ACTIVE
        );
    }

    public AccountResponse getAccountById(Long id){
        return new AccountResponse(id, "123", "Thu", BigDecimal.ZERO.setScale(2), AccountStatus.ACTIVE);
    }

    public List<AccountResponse> getAllAccounts(){
        return List.of(
            new AccountResponse (
                1L,
                    "ACC-001",
                    "Thu",
                    BigDecimal.ZERO.setScale(2),
                    AccountStatus.ACTIVE
            ),

            new AccountResponse(
                 2L,
                    "ACC-002",
                    "Nguyen",
                    BigDecimal.ZERO.setScale(2),
                    AccountStatus.ACTIVE)
        );
    }

    public AccountResponse updateAccount(Long id, UpdateAccountRequest request){
        String ownerName = request.getOwnerName();
        return new AccountResponse(
            id,
            "ACC-003",
            ownerName,
            BigDecimal.ZERO.setScale(2),
            AccountStatus.ACTIVE
        );
    }

    public void deleteAccount(Long id){

    }
}
