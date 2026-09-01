package com.chethu.paymentledgerservice.controller;

import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.TransactionService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    public TransactionController(TransactionService service){
        this.transactionService = service;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        }

        Page<TransactionResponse> response = transactionService.getHistoryForUser(
                principal.userId(), PageRequest.of(page, size, parseSort(sort)));
        return ResponseEntity.ok(response);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", -1);
        String property = parts[0].trim();
        if (!property.equals("createdAt") && !property.equals("id")
                && !property.equals("amount") && !property.equals("transactionType")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        if (parts.length > 2 || (parts.length == 2
                && !parts[1].trim().equalsIgnoreCase("asc")
                && !parts[1].trim().equalsIgnoreCase("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort direction");
        }
        Sort.Direction direction = parts.length == 2 && parts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
    
}

