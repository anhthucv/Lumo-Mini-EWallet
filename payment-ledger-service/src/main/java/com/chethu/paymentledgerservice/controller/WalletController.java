package com.chethu.paymentledgerservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.MyWalletResponse;
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.RecipientResponse;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.dto.WalletLimitsResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.AccountService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/wallet")
@Tag(name = "Wallet", description = "Authenticated wallet balances and money operations")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {
    private final AccountService accountService;

    public WalletController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my wallet")
    public ResponseEntity<MyWalletResponse> me(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        MyWalletResponse response = accountService.getMyWallet(principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/limits")
    @Operation(summary = "Get my wallet limits")
    public ResponseEntity<WalletLimitsResponse> limits(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return ResponseEntity.ok(accountService.getWalletLimits(principal.userId()));
    }

    @GetMapping("/recipient")
    @Operation(summary = "Look up a transfer recipient")
    public ResponseEntity<RecipientResponse> getRecipient(@RequestParam String accountNumber) {
        RecipientResponse response = accountService.getRecipient(accountNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit into the wallet")
    public ResponseEntity<AccountResponse> deposit(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MoneyOperationRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        AccountResponse response = accountService.depositForCurrentUser(principal.userId(), request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw from the wallet")
    public ResponseEntity<AccountResponse> withdraw(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody MoneyOperationRequest request
    ){
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        AccountResponse response = accountService.withdrawForCurrentUser(principal.userId(), request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds to another wallet")
    public ResponseEntity<AccountResponse> transfer(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        AccountResponse response = accountService.transferForCurrentUser(principal.userId(), request, idempotencyKey);
        return ResponseEntity.ok(response);
    }
    
}
