package com.chethu.paymentledgerservice.controller;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.dto.AdminAuditLogResponse;
import com.chethu.paymentledgerservice.service.AdminAuditLogService;
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {
    private final AdminAuditLogService service;
    public AdminAuditLogController(AdminAuditLogService service) { this.service = service; }
    @GetMapping
    public Page<AdminAuditLogResponse> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(search, action, from, to, pageable);
    }
    @GetMapping("/{id}") public AdminAuditLogResponse detail(@PathVariable Long id) { return service.detail(id); }
}
