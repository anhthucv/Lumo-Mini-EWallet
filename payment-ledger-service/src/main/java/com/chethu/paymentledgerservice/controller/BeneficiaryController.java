package com.chethu.paymentledgerservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.BeneficiaryResponse;
import com.chethu.paymentledgerservice.dto.CreateBeneficiaryRequest;
import com.chethu.paymentledgerservice.dto.UpdateBeneficiaryRequest;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.BeneficiaryService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/beneficiaries")
@Tag(name = "Beneficiaries", description = "Authenticated beneficiary management")
@SecurityRequirement(name = "bearerAuth")
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    @Operation(summary = "List my beneficiaries")
    public ResponseEntity<List<BeneficiaryResponse>> findAll(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.ok(beneficiaryService.findForCurrentUser(requirePrincipal(principal).userId()));
    }

    @PostMapping
    @Operation(summary = "Create a beneficiary")
    public ResponseEntity<BeneficiaryResponse> create(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateBeneficiaryRequest request) {
        BeneficiaryResponse response = beneficiaryService.createForCurrentUser(
                requirePrincipal(principal).userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update my beneficiary")
    public ResponseEntity<BeneficiaryResponse> update(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBeneficiaryRequest request) {
        BeneficiaryResponse response = beneficiaryService.updateForCurrentUser(
                requirePrincipal(principal).userId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my beneficiary")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id) {
        beneficiaryService.deleteForCurrentUser(requirePrincipal(principal).userId(), id);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUserPrincipal requirePrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return principal;
    }
}
