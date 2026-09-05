package com.chethu.paymentledgerservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.CurrentUserResponse;
import com.chethu.paymentledgerservice.dto.LoginRequest;
import com.chethu.paymentledgerservice.dto.LoginResponse;
import com.chethu.paymentledgerservice.dto.RegisterRequest;
import com.chethu.paymentledgerservice.dto.RegisterResponse;
import com.chethu.paymentledgerservice.dto.SendVerificationCodeRequest;
import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.EmailVerificationService;
import com.chethu.paymentledgerservice.service.LoginService;
import com.chethu.paymentledgerservice.service.RegistrationService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, email verification, login, and current-user access")
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
    @Operation(summary = "Send registration verification code")
    public ResponseEntity<VerificationCodeResponse> sendCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        VerificationCodeResponse response = emailVerificationService.sendRegistrationCode(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a verified user")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user")
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            Authentication authentication) {
        AuthenticatedUserPrincipal resolvedPrincipal = resolvePrincipal(principal, authentication);
        return ResponseEntity.ok(CurrentUserResponse.from(resolvedPrincipal));
    }

    private AuthenticatedUserPrincipal resolvePrincipal(AuthenticatedUserPrincipal principal,
            Authentication authentication) {
        if (principal != null) {
            return principal;
        }

        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal resolved) {
            return resolved;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
