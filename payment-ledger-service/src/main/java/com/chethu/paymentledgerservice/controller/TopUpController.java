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

import com.chethu.paymentledgerservice.dto.TopUpRequest;
import com.chethu.paymentledgerservice.dto.TopUpResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.TopUpService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/topups")
public class TopUpController {
    private final TopUpService topUpService;

    public TopUpController(TopUpService topUpService) {
        this.topUpService = topUpService;
    }

    @PostMapping
    public ResponseEntity<TopUpResponse> create(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TopUpRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(topUpService.createForCurrentUser(principal.userId(), request, idempotencyKey));
    }
}
