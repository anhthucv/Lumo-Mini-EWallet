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

import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.exception.DuplicateEmailException;
import com.chethu.paymentledgerservice.exception.GlobalExceptionHandler;
import com.chethu.paymentledgerservice.exception.VerificationCodeResendTooSoonException;
import com.chethu.paymentledgerservice.service.EmailVerificationService;

public class AuthControllerTest {
    private MockMvc mockMvc;
    private StubEmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new StubEmailVerificationService();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void sendCode_shouldReturnBadRequest_forMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VALIDATION_ERROR")));
    }

    @Test
    void sendCode_shouldReturnOk_forValidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Verification code sent")));
    }

    @Test
    void sendCode_shouldReturnConflict_forDuplicateEmail() throws Exception {
        service.throwable = new DuplicateEmailException("user@example.com");

        mockMvc.perform(post("/api/auth/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DUPLICATE_EMAIL")));
    }

    @Test
    void sendCode_shouldReturnTooManyRequests_forCooldown() throws Exception {
        service.throwable = new VerificationCodeResendTooSoonException();

        mockMvc.perform(post("/api/auth/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("VERIFICATION_CODE_RESEND_TOO_SOON")));
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
}
