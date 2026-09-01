package com.chethu.paymentledgerservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.ChangePasswordRequest;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.PasswordService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users/me")
public class UserPasswordController {
    private final PasswordService passwordService;

    public UserPasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        passwordService.changePassword(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
