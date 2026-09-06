package com.chethu.paymentledgerservice.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.extension.ExtendWith;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.config.SecurityConfig;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.repository.UserRepository;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, SecurityConfigTest.TestConfig.class})
class SecurityConfigTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UNAUTHORIZED")));
    }

    @Test
    void protectedEndpoint_shouldAllowValidBearerToken() throws Exception {
        UserEntity user = user(31L, "user@example.com", UserRole.USER, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);

        mockMvc.perform(get("/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("protected ok")));
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedForTamperedToken() throws Exception {
        UserEntity user = user(32L, "user@example.com", UserRole.USER, UserStatus.ACTIVE);
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        mockMvc.perform(get("/protected").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UNAUTHORIZED")));
    }

    @Test
    void adminEndpoint_shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UNAUTHORIZED")));
    }

    @Test
    void adminEndpoint_shouldReturnUnauthorizedForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("UNAUTHORIZED")));
    }

    @Test
    void adminEndpoint_shouldReturnForbiddenForUserRole() throws Exception {
        UserEntity user = user(33L, "user@example.com", UserRole.USER, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);

        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FORBIDDEN")));
    }

    @Test
    void adminEndpoint_shouldAllowAdminRole() throws Exception {
        UserEntity admin = user(34L, "admin@example.com", UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        String token = jwtService.generateAccessToken(admin);

        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin ok")));
    }

    @Test
    void publicAuthEndpoint_shouldRemainPublic() throws Exception {
        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("public auth ok")));
    }

    @Test
    void publicTopUpWebhook_shouldRemainPublic() throws Exception {
        mockMvc.perform(post("/topups/webhook"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("public webhook ok")));
    }

    @Test
    void allowedFrontendOrigin_shouldPassCorsPreflightWithRequiredMethodsAndHeaders() throws Exception {
        mockMvc.perform(options("/wallet/me")
                .header("Origin", "https://lumo-mini-e-wallet.vercel.app")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type,X-Request-Id,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://lumo-mini-e-wallet.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("GET"),
                                org.hamcrest.Matchers.containsString("POST"),
                                org.hamcrest.Matchers.containsString("PUT"),
                                org.hamcrest.Matchers.containsString("PATCH"),
                                org.hamcrest.Matchers.containsString("DELETE"),
                                org.hamcrest.Matchers.containsString("OPTIONS"))))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("Authorization"),
                                org.hamcrest.Matchers.containsString("Content-Type"),
                                org.hamcrest.Matchers.containsString("X-Request-Id"),
                                org.hamcrest.Matchers.containsString("Idempotency-Key"))));
    }

    @Test
    void unapprovedOrigin_shouldNotReceiveCorsPermission() throws Exception {
        mockMvc.perform(options("/wallet/me")
                .header("Origin", "https://malicious.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void allowedOrigin_doesNotBypassProtectedRoute() throws Exception {
        mockMvc.perform(get("/protected")
                .header("Origin", "https://lumo-mini-e-wallet.vercel.app"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://lumo-mini-e-wallet.vercel.app"));
    }

    private UserEntity user(Long id, String email, UserRole role, UserStatus status) {
        UserEntity user = new UserEntity(email, "hash", "Nguyen Van A", role, status);
        try {
            Field field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean
        JwtService jwtService() {
            return new JwtService("payment-ledger-test-jwt-secret-change-me-to-a-long-enough-value", 3600000L);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
            return new JwtAuthenticationFilter(jwtService, userRepository);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
            return new JwtAuthenticationEntryPoint();
        }

        @Bean
        JwtAccessDeniedHandler jwtAccessDeniedHandler() {
            return new JwtAccessDeniedHandler();
        }

        @Bean
        ProtectedController protectedController() {
            return new ProtectedController();
        }
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/protected")
        Map<String, String> protectedEndpoint() {
            return Map.of("message", "protected ok");
        }

        @GetMapping("/api/admin/test")
        Map<String, String> adminEndpoint() {
            return Map.of("message", "admin ok");
        }

        @org.springframework.web.bind.annotation.PostMapping("/auth/login")
        Map<String, String> publicAuthEndpoint() {
            return Map.of("message", "public auth ok");
        }

        @org.springframework.web.bind.annotation.PostMapping("/topups/webhook")
        Map<String, String> publicWebhookEndpoint() {
            return Map.of("message", "public webhook ok");
        }
    }
}
