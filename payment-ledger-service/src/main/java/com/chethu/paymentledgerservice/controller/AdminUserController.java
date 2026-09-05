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

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<AdminUserResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return adminUserService.listUsers(search, pageable);
    }

    @PostMapping("/{userId}/lock")
    public ResponseEntity<AdminUserResponse> lock(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusChangeRequest request) {
        return ResponseEntity.ok(adminUserService.lockUser(principal.userId(), userId, request.reason()));
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<AdminUserResponse> unlock(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusChangeRequest request) {
        return ResponseEntity.ok(adminUserService.unlockUser(principal.userId(), userId, request.reason()));
    }
}
