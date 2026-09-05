package com.chethu.paymentledgerservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.AdminUserResponse;
import com.chethu.paymentledgerservice.exception.GlobalExceptionHandler;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.AdminUserService;

class AdminUserControllerTest {
    private MockMvc mockMvc;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = mock(AdminUserService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @Test
    void lock_shouldRejectBlankReason() throws Exception {
        mockMvc.perform(post("/api/admin/users/7/lock")
                .with(adminPrincipal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));
    }

    @Test
    void lock_shouldRejectReasonLongerThan255Characters() throws Exception {
        String reason = "x".repeat(256);
        mockMvc.perform(post("/api/admin/users/7/lock")
                .with(adminPrincipal())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));
    }

    @Test
    void list_shouldReturnSafeAdminUserResponse() {
        AdminUserResponse response = new AdminUserResponse(7L, "user@example.com", "User", UserRole.USER,
                UserStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0), 8L, "****7890", AccountStatus.ACTIVE,
                java.math.BigDecimal.TEN);
        when(service.listUsers(any(), any())).thenReturn(new PageImpl<>(List.of(response)));

        org.springframework.data.domain.Page<AdminUserResponse> result = new AdminUserController(service)
                .list(null, org.springframework.data.domain.PageRequest.of(0, 10));

        org.junit.jupiter.api.Assertions.assertEquals("****7890", result.getContent().get(0).accountNumberSummary());
        org.junit.jupiter.api.Assertions.assertEquals(10, AdminUserResponse.class.getRecordComponents().length);
    }

    private RequestPostProcessor adminPrincipal() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(1L, "admin@example.com", "Admin",
                UserRole.ADMIN, UserStatus.ACTIVE);
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(principal, null));
            return request;
        };
    }
}
