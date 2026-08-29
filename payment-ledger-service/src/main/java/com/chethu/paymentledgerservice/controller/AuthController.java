package com.chethu.paymentledgerservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethu.paymentledgerservice.dto.SendVerificationCodeRequest;
import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.service.EmailVerificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final EmailVerificationService emailVerificationService;

    public AuthController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register/send-code")
    public ResponseEntity<VerificationCodeResponse> sendCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        VerificationCodeResponse response = emailVerificationService.sendRegistrationCode(request.getEmail());
        return ResponseEntity.ok(response);
    }
}
