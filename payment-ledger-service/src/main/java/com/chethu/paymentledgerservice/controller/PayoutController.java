package com.chethu.paymentledgerservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.PayoutRequest;
import com.chethu.paymentledgerservice.dto.PayoutResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.PayoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payouts")
public class PayoutController {
    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) { this.payoutService = payoutService; }

    @PostMapping
    public ResponseEntity<PayoutResponse> create(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PayoutRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(payoutService.createForCurrentUser(principal.userId(), request, idempotencyKey));
    }
}
