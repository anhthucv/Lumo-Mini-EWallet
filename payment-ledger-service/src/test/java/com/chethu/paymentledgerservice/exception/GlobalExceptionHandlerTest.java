package com.chethu.paymentledgerservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.ErrorResponse;

class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/wallet/deposit");
    }

    @Test
    void resourceNotFound_shouldUseStandardContract() {
        ErrorResponse response = handler.handleAccountNotFound(new AccountNotFoundException(42L), request).getBody();

        assertStandard(response, 404, "ACCOUNT_NOT_FOUND", "/wallet/deposit");
        assertNull(response.getFieldErrors());
    }

    @Test
    void businessAndStatusErrors_shouldUseStandardContract() {
        ErrorResponse invalid = handler.handleInvalidTransferException(
                new InvalidTransferException("Recipient account number must not be blank"), request).getBody();
        ErrorResponse inactive = handler.handleAccountNotActive(new AccountNotActiveException(), request).getBody();

        assertStandard(invalid, 400, "INVALID_TRANSFER", "/wallet/deposit");
        assertStandard(inactive, 403, "ACCOUNT_NOT_ACTIVE", "/wallet/deposit");
    }

    @Test
    void validation_shouldIncludeSafeFieldErrorsWithoutRejectedValues() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "currentPassword", "Current password must not be blank"),
                new FieldError("request", "newPassword", "New password must be at least 8 characters long")));
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ErrorResponse response = handler.handleMethodArgumentNotValid(exception, request).getBody();

        assertStandard(response, 400, "VALIDATION_ERROR", "/wallet/deposit");
        assertEquals("Validation failed", response.getMessage());
        assertEquals(Map.of(
                "currentPassword", "Current password must not be blank",
                "newPassword", "New password must be at least 8 characters long"), response.getFieldErrors());
        assertFalse(response.toString().contains("current-secret"));
        assertFalse(response.toString().contains("passwordHash"));
    }

    @Test
    void malformedRequest_shouldUseSafeBadRequestContract() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

        ErrorResponse response = handler.handleMalformedRequest(exception, request).getBody();

        assertStandard(response, 400, "BAD_REQUEST", "/wallet/deposit");
        assertEquals("Request parameters or body are invalid", response.getMessage());
    }

    @Test
    void responseStatusException_shouldUseStandardContractWithoutInternalDetails() {
        ErrorResponse response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values"), request).getBody();

        assertStandard(response, 400, "BAD_REQUEST", "/wallet/deposit");
        assertEquals("Invalid pagination values", response.getMessage());
    }

    @Test
    void unexpectedException_shouldReturnSafeFiveHundredResponse() {
        ErrorResponse response = handler.handleUnexpectedException(
                new IllegalStateException("database password leaked"), request).getBody();

        assertStandard(response, 500, "INTERNAL_SERVER_ERROR", "/wallet/deposit");
        assertEquals("An unexpected error occurred", response.getMessage());
        assertFalse(response.getMessage().contains("database password"));
    }

    private void assertStandard(ErrorResponse response, int status, String error, String path) {
        assertNotNull(response);
        assertNotNull(response.getTimestamp());
        assertEquals(status, response.getStatus());
        assertEquals(error, response.getError());
        assertNotNull(response.getMessage());
        assertEquals(path, response.getPath());
        assertTrue(response.getFieldErrors() == null || !response.getFieldErrors().isEmpty());
    }
}
