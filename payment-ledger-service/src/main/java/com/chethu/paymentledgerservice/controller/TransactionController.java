package com.chethu.paymentledgerservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.service.TransactionService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    public TransactionController(TransactionService service){
        this.transactionService = service;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getHistoryByAccount(@RequestParam Long accountId, Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getHistoryByAccount(accountId, pageable);
        return ResponseEntity.ok(response);
    }
    
}


