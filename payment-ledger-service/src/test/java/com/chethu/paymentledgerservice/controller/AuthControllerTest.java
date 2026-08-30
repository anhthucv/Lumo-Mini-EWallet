package com.chethu.paymentledgerservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.chethu.paymentledgerservice.dto.RegisterResponse;
import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.exception.DuplicateEmailException;
import com.chethu.paymentledgerservice.exception.GlobalExceptionHandler;
import com.chethu.paymentledgerservice.exception.InvalidVerificationCodeException;
import com.chethu.paymentledgerservice.exception.VerificationCodeExpiredException;
import com.chethu.paymentledgerservice.exception.VerificationCodeNotFoundException;
import com.chethu.paymentledgerservice.exception.VerificationCodeResendTooSoonException;
import com.chethu.paymentledgerservice.service.EmailVerificationService;
import com.chethu.paymentledgerservice.service.RegistrationService;

import java.math.BigDecimal;

public class AuthControllerTest {
    private MockMvc mockMvc;
    private StubEmailVerificationService emailVerificationService;
    private StubRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new StubEmailVerificationService();
        registrationService = new StubRegistrationService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(emailVerificationService, registrationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void sendCode_shouldReturnBadRequest_forMalformedEmail() throws Exception {
        mockMvc.perform(post("/auth/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));
    }

    @Test
    void sendCode_shouldReturnOk_forValidRequest() throws Exception {
        mockMvc.perform(post("/auth/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Verification code sent")));
    }

    @Test
    void register_shouldReturnCreated_forValidRequest() throws Exception {
        registrationService.response = new RegisterResponse(
                1L,
                "user@example.com",
                "Nguyen Van A",
                2L,
                "ACC-123456789012",
                BigDecimal.ZERO,
                UserRole.USER,
                UserStatus.ACTIVE,
                AccountStatus.ACTIVE);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "user@example.com",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "123456"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ACC-123456789012")));
    }

    @Test
    void register_shouldReturnBadRequest_forInvalidOtpFormat() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "user@example.com",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "12"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));
    }

    @Test
    void register_shouldReturnConflict_forDuplicateEmail() throws Exception {
        registrationService.throwable = new DuplicateEmailException("user@example.com");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "user@example.com",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "123456"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DUPLICATE_EMAIL")));
    }

    @Test
    void register_shouldReturnNotFound_forMissingOtp() throws Exception {
        registrationService.throwable = new VerificationCodeNotFoundException("user@example.com");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "user@example.com",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "123456"
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VERIFICATION_CODE_NOT_FOUND")));
    }

    @Test
    void register_shouldReturnBadRequest_forExpiredOtp() throws Exception {
        registrationService.throwable = new VerificationCodeExpiredException();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "user@example.com",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "123456"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VERIFICATION_CODE_EXPIRED")));
    }

    @Test
    void register_shouldReturnBadRequest_forInvalidOtp() throws Exception {
        registrationService.throwable = new InvalidVerificationCodeException();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "user@example.com",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "654321"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INVALID_VERIFICATION_CODE")));
    }

    @Test
    void register_shouldReturnBadRequest_forMalformedEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "not-an-email",
                          "password": "some-password",
                          "fullName": "Nguyen Van A",
                          "code": "123456"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));
    }

    private static final class StubEmailVerificationService extends EmailVerificationService {
        private RuntimeException throwable;

        private StubEmailVerificationService() {
            super(null, null, null, null, null);
        }

        @Override
        public VerificationCodeResponse sendRegistrationCode(String email) {
            if (throwable != null) {
                throw throwable;
            }
            return new VerificationCodeResponse("Verification code sent");
        }
    }

    private static final class StubRegistrationService extends RegistrationService {
        private RuntimeException throwable;
        private RegisterResponse response;

        private StubRegistrationService() {
            super(null, null, null, null, null);
        }

        @Override
        public RegisterResponse register(com.chethu.paymentledgerservice.dto.RegisterRequest request) {
            if (throwable != null) {
                throw throwable;
            }
            return response;
        }
    }
}
