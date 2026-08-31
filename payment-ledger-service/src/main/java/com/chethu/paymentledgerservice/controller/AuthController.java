package com.chethu.paymentledgerservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethu.paymentledgerservice.dto.LoginRequest;
import com.chethu.paymentledgerservice.dto.LoginResponse;
import com.chethu.paymentledgerservice.dto.RegisterRequest;
import com.chethu.paymentledgerservice.dto.RegisterResponse;
import com.chethu.paymentledgerservice.dto.SendVerificationCodeRequest;
import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.service.EmailVerificationService;
import com.chethu.paymentledgerservice.service.LoginService;
import com.chethu.paymentledgerservice.service.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final EmailVerificationService emailVerificationService;
    private final RegistrationService registrationService;
    private final LoginService loginService;

    public AuthController(EmailVerificationService emailVerificationService, RegistrationService registrationService,
            LoginService loginService) {
        this.emailVerificationService = emailVerificationService;
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/register/send-code")
    public ResponseEntity<VerificationCodeResponse> sendCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        VerificationCodeResponse response = emailVerificationService.sendRegistrationCode(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.ok(response);
    }
}
