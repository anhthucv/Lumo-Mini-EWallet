package com.chethu.paymentledgerservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chethu.paymentledgerservice.dto.AdminUserResponse;
import com.chethu.paymentledgerservice.dto.AdminUserStatusChangeRequest;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.AdminUserService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin Users", description = "ADMIN-only user administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List users")
    public Page<AdminUserResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return adminUserService.listUsers(search, pageable);
    }

    @PostMapping("/{userId}/lock")
    @Operation(summary = "Lock a user")
    public ResponseEntity<AdminUserResponse> lock(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusChangeRequest request) {
        return ResponseEntity.ok(adminUserService.lockUser(principal.userId(), userId, request.reason()));
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "Unlock a user")
    public ResponseEntity<AdminUserResponse> unlock(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusChangeRequest request) {
        return ResponseEntity.ok(adminUserService.unlockUser(principal.userId(), userId, request.reason()));
    }
}
