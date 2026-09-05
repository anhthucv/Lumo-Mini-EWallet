package com.chethu.paymentledgerservice.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chethu.paymentledgerservice.dto.AdminDashboardResponse;
import com.chethu.paymentledgerservice.service.AdminDashboardService;
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService service;
    public AdminDashboardController(AdminDashboardService service) { this.service = service; }
    @GetMapping
    public AdminDashboardResponse get() { return service.getDashboard(); }
}
