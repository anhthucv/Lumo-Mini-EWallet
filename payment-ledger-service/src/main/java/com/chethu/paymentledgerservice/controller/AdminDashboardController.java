package com.chethu.paymentledgerservice.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chethu.paymentledgerservice.dto.AdminDashboardResponse;
import com.chethu.paymentledgerservice.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "ADMIN-only operational dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {
    private final AdminDashboardService service;
    public AdminDashboardController(AdminDashboardService service) { this.service = service; }
    @GetMapping
    @Operation(summary = "Get the admin dashboard")
    public AdminDashboardResponse get() { return service.getDashboard(); }
}
