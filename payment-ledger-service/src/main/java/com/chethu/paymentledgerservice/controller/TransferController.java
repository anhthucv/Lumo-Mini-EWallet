package com.chethu.paymentledgerservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.dto.TransferResponse;
import com.chethu.paymentledgerservice.service.TransferService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final TransferService transferService;
    public TransferController(TransferService transferService){
        this.transferService = transferService;
    }

    @PostMapping
    ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request){
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(response);
    }

}
