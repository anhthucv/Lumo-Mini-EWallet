package com.chethu.paymentledgerservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;

import com.chethu.paymentledgerservice.dto.TopUpRequest;
import com.chethu.paymentledgerservice.dto.TopUpResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.TopUpService;
import com.chethu.paymentledgerservice.service.TopUpStatusSyncService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/topups")
public class TopUpController {
    private final TopUpService topUpService;
    private final TopUpStatusSyncService topUpStatusSyncService;

    @Autowired
    public TopUpController(TopUpService topUpService, TopUpStatusSyncService topUpStatusSyncService) {
        this.topUpService = topUpService;
        this.topUpStatusSyncService = topUpStatusSyncService;
    }

    public TopUpController(TopUpService topUpService) {
        this(topUpService, null);
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

    @GetMapping("/{id}")
    public TopUpResponse get(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id) {
        requirePrincipal(principal);
        return topUpService.getForCurrentUser(principal.userId(), id);
    }

    @PostMapping("/{id}/sync")
    public TopUpResponse sync(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id) {
        requirePrincipal(principal);
        return topUpStatusSyncService.syncForCurrentUser(principal.userId(), id);
    }

    private void requirePrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
