package com.chethu.paymentledgerservice.controller;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.AdminTransactionDetailResponse;
import com.chethu.paymentledgerservice.dto.AdminTransactionResponse;
import com.chethu.paymentledgerservice.service.AdminTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin/transactions")
@Tag(name = "Admin Transactions", description = "ADMIN-only transaction investigation")
@SecurityRequirement(name = "bearerAuth")
public class AdminTransactionController {
    private final AdminTransactionService service;
    public AdminTransactionController(AdminTransactionService service) { this.service = service; }
    @GetMapping
    @Operation(summary = "List all transactions for administration")
    public Page<AdminTransactionResponse> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) TransactionType type, @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(search, type, status, from, to, pageable);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get an administrative transaction detail")
    public ResponseEntity<AdminTransactionDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.detail(id));
    }
}
