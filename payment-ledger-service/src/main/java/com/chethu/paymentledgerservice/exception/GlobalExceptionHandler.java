package com.chethu.paymentledgerservice.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingPathVariableException.class, HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request parameters or body are invalid", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = switch (status) {
            case UNAUTHORIZED -> "Authentication required";
            case FORBIDDEN -> "Access denied";
            default -> status.is5xxServerError() ? "An unexpected error occurred" : ex.getReason();
        };
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }
        return error(status, status.name(), message, request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException ex,
            HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransactionFilterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransactionFilter(InvalidTransactionFilterException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_FILTER", ex.getMessage(), request);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransactionStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransactionStatusTransition(
            InvalidTransactionStatusTransitionException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "INVALID_TRANSACTION_STATUS_TRANSITION", ex.getMessage(), request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Authenticated user could not be found", request);
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAccountNumberException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccountNumber(InvalidAccountNumberException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_NUMBER", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransferException(InvalidTransferException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TRANSFER", ex.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex,
            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(PerTransactionLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePerTransactionLimitExceeded(
            PerTransactionLimitExceededException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "PER_TRANSACTION_LIMIT_EXCEEDED", ex.getMessage(), request);
    }

    @ExceptionHandler(DailyTransactionLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDailyTransactionLimitExceeded(
            DailyTransactionLimitExceededException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DAILY_TRANSACTION_LIMIT_EXCEEDED", ex.getMessage(), request);
    }

    @ExceptionHandler(RiskRejectedException.class)
    public ResponseEntity<ErrorResponse> handleRiskRejected(RiskRejectedException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "RISK_REJECTED", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request);
    }

    @ExceptionHandler(VerificationCodeResendTooSoonException.class)
    public ResponseEntity<ErrorResponse> handleVerificationCodeResendTooSoon(
            VerificationCodeResendTooSoonException ex, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "VERIFICATION_CODE_RESEND_TOO_SOON", ex.getMessage(), request);
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> handleMailException(MailException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED", "Unable to send verification email", request);
    }

    @ExceptionHandler(VerificationCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVerificationCodeNotFound(VerificationCodeNotFoundException ex,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "VERIFICATION_CODE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleVerificationCodeExpired(VerificationCodeExpiredException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_EXPIRED", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCode(InvalidVerificationCodeException ex,
            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
            HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler(UserLockedException.class)
    public ResponseEntity<ErrorResponse> handleUserLocked(UserLockedException ex, HttpServletRequest request) {
        return error(HttpStatus.LOCKED, "USER_LOCKED", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected {} while processing {}", ex.getClass().getSimpleName(), request.getRequestURI());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String category, String message,
            HttpServletRequest request) {
        return error(status, category, message, request, null);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String category, String message,
            HttpServletRequest request, Map<String, String> fieldErrors) {
        ErrorResponse response = new ErrorResponse(Instant.now(), status.value(), category, message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(response);
    }
}
