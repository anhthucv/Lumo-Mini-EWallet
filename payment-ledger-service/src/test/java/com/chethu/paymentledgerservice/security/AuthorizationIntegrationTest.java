package com.chethu.paymentledgerservice.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.junit.jupiter.api.extension.ExtendWith;

import com.chethu.paymentledgerservice.config.SecurityConfig;
import com.chethu.paymentledgerservice.controller.AdminAuditLogController;
import com.chethu.paymentledgerservice.controller.AdminDashboardController;
import com.chethu.paymentledgerservice.controller.AdminTransactionController;
import com.chethu.paymentledgerservice.controller.AdminUserController;
import com.chethu.paymentledgerservice.controller.WalletController;
import com.chethu.paymentledgerservice.controller.BeneficiaryController;
import com.chethu.paymentledgerservice.controller.NotificationController;
import com.chethu.paymentledgerservice.controller.TopUpController;
import com.chethu.paymentledgerservice.controller.TransactionController;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.repository.UserRepository;
import com.chethu.paymentledgerservice.service.AccountService;
import com.chethu.paymentledgerservice.service.AdminAuditLogService;
import com.chethu.paymentledgerservice.service.AdminDashboardService;
import com.chethu.paymentledgerservice.service.AdminTransactionService;
import com.chethu.paymentledgerservice.service.AdminUserService;
import com.chethu.paymentledgerservice.service.BeneficiaryService;
import com.chethu.paymentledgerservice.service.NotificationPersistenceService;
import com.chethu.paymentledgerservice.service.TopUpService;
import com.chethu.paymentledgerservice.service.TopUpStatusSyncService;
import com.chethu.paymentledgerservice.service.TransactionService;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SecurityConfig.class, AuthorizationIntegrationTest.TestConfig.class })
class AuthorizationIntegrationTest {
    @Autowired private org.springframework.web.context.WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private AccountService accountService;
    @Autowired private AdminUserService adminUserService;
    @Autowired private AdminDashboardService adminDashboardService;
    @Autowired private AdminTransactionService adminTransactionService;
    @Autowired private AdminAuditLogService adminAuditLogService;
    @Autowired private TransactionService transactionService;
    @Autowired private BeneficiaryService beneficiaryService;
    @Autowired private NotificationPersistenceService notificationService;
    @Autowired private TopUpService topUpService;
    @Autowired private TopUpStatusSyncService topUpStatusSyncService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build();
    }

    @Test
    void adminUsers_shouldAllowAdminAndRejectUserAndAnonymous() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin1@example.com")).thenReturn(Optional.of(admin(1L)));
        when(userRepository.findByEmailIgnoreCase("user2@example.com")).thenReturn(Optional.of(user(2L)));
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(admin(1L)))).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(user(2L)))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminControllers_shouldAllowAdminOnly() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin1@example.com")).thenReturn(Optional.of(admin(1L)));
        when(userRepository.findByEmailIgnoreCase("user2@example.com")).thenReturn(Optional.of(user(2L)));
        String adminToken = bearer(admin(1L));
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/transactions").header("Authorization", adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", adminToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(user(2L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidJwt_shouldReturnUnauthorizedForActualAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockedUser_shouldLoseAccessWithPreviouslyIssuedJwt() throws Exception {
        UserEntity locked = user(2L);
        String token = jwtService.generateAccessToken(locked);
        when(userRepository.findByEmailIgnoreCase(locked.getEmail())).thenReturn(Optional.of(locked));

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedWalletRoute_shouldAllowAuthenticatedUserButNotAnonymous() throws Exception {
        when(userRepository.findByEmailIgnoreCase("user2@example.com")).thenReturn(Optional.of(user(2L)));
        mockMvc.perform(get("/wallet/me").header("Authorization", bearer(user(2L)))).andExpect(status().isOk());
        mockMvc.perform(get("/wallet/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void userOwnedRoutes_shouldUseAuthenticatedUserIdAndRejectAnonymous() throws Exception {
        when(userRepository.findByEmailIgnoreCase("user2@example.com")).thenReturn(Optional.of(user(2L)));
        String token = bearer(user(2L));

        mockMvc.perform(get("/transactions/91").header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get("/beneficiaries").header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get("/notifications/unread-count").header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get("/topups/91").header("Authorization", token)).andExpect(status().isOk());

        verify(transactionService).getTransactionForUser(2L, 91L);
        verify(beneficiaryService).findForCurrentUser(2L);
        verify(notificationService).unreadCountForUser(2L);
        verify(topUpService).getForCurrentUser(2L, 91L);

        mockMvc.perform(get("/transactions/91")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/beneficiaries")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/topups/91")).andExpect(status().isUnauthorized());
        verify(transactionService, never()).getTransactionForUser(eq(null), eq(91L));
    }

    @Test
    void protectedWalletValidation_shouldRejectInvalidAmountBeforeService() throws Exception {
        when(userRepository.findByEmailIgnoreCase("user2@example.com")).thenReturn(Optional.of(user(2L)));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/wallet/deposit")
                .header("Authorization", bearer(user(2L)))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"amount\":0.50}"))
                .andExpect(status().isBadRequest());
        verify(accountService, never()).depositForCurrentUser(eq(2L), any(), any());
    }

    private String bearer(UserEntity user) { return "Bearer " + jwtService.generateAccessToken(user); }
    private UserEntity user(Long id) { return user(id, UserRole.USER); }
    private UserEntity admin(Long id) { return user(id, UserRole.ADMIN); }
    private UserEntity user(Long id, UserRole role) {
        UserEntity entity = new UserEntity((role == UserRole.ADMIN ? "admin" : "user") + id + "@example.com", "hash", "Test User", role, UserStatus.ACTIVE);
        try { var field = UserEntity.class.getDeclaredField("id"); field.setAccessible(true); field.set(entity, id); return entity; }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean JwtService jwtService() { return new JwtService("payment-ledger-integration-test-secret-long-enough", 3600000L); }
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean com.chethu.paymentledgerservice.security.JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwt, UserRepository users) { return new com.chethu.paymentledgerservice.security.JwtAuthenticationFilter(jwt, users); }
        @Bean com.chethu.paymentledgerservice.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() { return new com.chethu.paymentledgerservice.security.JwtAuthenticationEntryPoint(); }
        @Bean com.chethu.paymentledgerservice.security.JwtAccessDeniedHandler jwtAccessDeniedHandler() { return new com.chethu.paymentledgerservice.security.JwtAccessDeniedHandler(); }
        @Bean AdminUserService adminUserService() { return mock(AdminUserService.class); }
        @Bean AdminDashboardService adminDashboardService() { return mock(AdminDashboardService.class); }
        @Bean AdminTransactionService adminTransactionService() { return mock(AdminTransactionService.class); }
        @Bean AdminAuditLogService adminAuditLogService() { return mock(AdminAuditLogService.class); }
        @Bean AccountService accountService() { return mock(AccountService.class); }
        @Bean TransactionService transactionService() { return mock(TransactionService.class); }
        @Bean BeneficiaryService beneficiaryService() { return mock(BeneficiaryService.class); }
        @Bean NotificationPersistenceService notificationService() { return mock(NotificationPersistenceService.class); }
        @Bean TopUpService topUpService() { return mock(TopUpService.class); }
        @Bean TopUpStatusSyncService topUpStatusSyncService() { return mock(TopUpStatusSyncService.class); }
        @Bean WebMvcConfigurer pageableResolver() {
            return new WebMvcConfigurer() {
                @Override
                public void addArgumentResolvers(List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
                    resolvers.add(new PageableHandlerMethodArgumentResolver());
                }
            };
        }
        @Bean AdminUserController adminUserController(AdminUserService service) { return new AdminUserController(service); }
        @Bean AdminDashboardController adminDashboardController(AdminDashboardService service) { return new AdminDashboardController(service); }
        @Bean AdminTransactionController adminTransactionController(AdminTransactionService service) { return new AdminTransactionController(service); }
        @Bean AdminAuditLogController adminAuditLogController(AdminAuditLogService service) { return new AdminAuditLogController(service); }
        @Bean WalletController walletController(AccountService service) { return new WalletController(service); }
        @Bean TransactionController transactionController(TransactionService service) { return new TransactionController(service); }
        @Bean BeneficiaryController beneficiaryController(BeneficiaryService service) { return new BeneficiaryController(service); }
        @Bean NotificationController notificationController(NotificationPersistenceService service) { return new NotificationController(service); }
        @Bean TopUpController topUpController(TopUpService service, TopUpStatusSyncService syncService) { return new TopUpController(service, syncService); }
    }
}
