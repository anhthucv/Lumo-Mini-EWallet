package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.EmailVerificationStatus;
import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.RegisterRequest;
import com.chethu.paymentledgerservice.dto.RegisterResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.EmailVerificationEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.DuplicateEmailException;
import com.chethu.paymentledgerservice.exception.InvalidVerificationCodeException;
import com.chethu.paymentledgerservice.exception.VerificationCodeExpiredException;
import com.chethu.paymentledgerservice.exception.VerificationCodeNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.EmailVerificationRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private EmailVerificationRepository emailVerificationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                accountRepository,
                emailVerificationRepository,
                passwordEncoder,
                accountNumberGenerator);
    }

    @Test
    void register_success_shouldCreateUserWalletAndMarkOtpUsed() {
        String rawEmail = "  User@Example.com  ";
        String normalizedEmail = "user@example.com";
        EmailVerificationEntity verification = verification(normalizedEmail, "otp-hash", EmailVerificationStatus.ACTIVE, 0,
                LocalDateTime.now().plusMinutes(5));
        AtomicReference<UserEntity> savedUser = new AtomicReference<>();
        AtomicReference<AccountEntity> savedAccount = new AtomicReference<>();
        AtomicReference<EmailVerificationEntity> savedVerification = new AtomicReference<>();

        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(emailVerificationRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true);
        when(passwordEncoder.encode("some-password")).thenReturn("encoded-password");
        when(accountNumberGenerator.generateUniqueAccountNumber()).thenReturn("ACC-123456789012");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            setId(user, 1L);
            savedUser.set(user);
            return user;
        });
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity account = invocation.getArgument(0);
            setId(account, 2L);
            savedAccount.set(account);
            return account;
        });
        when(emailVerificationRepository.save(any(EmailVerificationEntity.class))).thenAnswer(invocation -> {
            EmailVerificationEntity entity = invocation.getArgument(0);
            savedVerification.set(entity);
            return entity;
        });

        RegisterResponse response = registrationService.register(registerRequest(rawEmail, "some-password", "Nguyen Van A", "123456"));

        assertEquals(1L, response.getUserId());
        assertEquals(normalizedEmail, response.getEmail());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals(2L, response.getAccountId());
        assertEquals("ACC-123456789012", response.getAccountNumber());
        assertEquals(0, response.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(UserRole.USER, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getUserStatus());
        assertEquals(AccountStatus.ACTIVE, response.getAccountStatus());

        assertNotNull(savedUser.get());
        assertNotNull(savedAccount.get());
        assertNotNull(savedVerification.get());
        assertEquals(normalizedEmail, savedUser.get().getEmail());
        assertEquals("encoded-password", savedUser.get().getPasswordHash());
        assertEquals(UserRole.USER, savedUser.get().getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.get().getStatus());
        assertEquals("Nguyen Van A", savedAccount.get().getOwnerName());
        assertEquals("ACC-123456789012", savedAccount.get().getAccountNumber());
        assertEquals(0, savedAccount.get().getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(savedUser.get(), savedAccount.get().getUser());
        assertEquals(EmailVerificationStatus.USED, savedVerification.get().getStatus());
        assertEquals(0, savedVerification.get().getFailedAttempts());

        verify(userRepository).existsByEmailIgnoreCase(normalizedEmail);
        verify(emailVerificationRepository).findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE);
        verify(passwordEncoder).matches("123456", "otp-hash");
        verify(passwordEncoder).encode("some-password");
        verify(accountNumberGenerator).generateUniqueAccountNumber();
        verify(userRepository).save(any(UserEntity.class));
        verify(accountRepository).save(any(AccountEntity.class));
        verify(emailVerificationRepository).save(verification);
    }

    @Test
    void register_duplicateEmail_shouldNotCallSaveMethods() {
        String normalizedEmail = "user@example.com";
        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> registrationService.register(registerRequest(normalizedEmail, "some-password", "Nguyen Van A", "123456")));

        assertTrue(ex.getMessage().contains(normalizedEmail));
        verify(userRepository).existsByEmailIgnoreCase(normalizedEmail);
        verify(userRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(emailVerificationRepository, passwordEncoder, accountNumberGenerator);
    }

    @Test
    void register_withoutActiveOtp_shouldThrowNotFound() {
        String normalizedEmail = "user@example.com";
        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(emailVerificationRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(VerificationCodeNotFoundException.class,
                () -> registrationService.register(registerRequest(normalizedEmail, "some-password", "Nguyen Van A", "123456")));

        verify(userRepository).existsByEmailIgnoreCase(normalizedEmail);
        verify(emailVerificationRepository).findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE);
        verify(emailVerificationRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_expiredOtp_shouldMarkVerificationExpired() {
        String normalizedEmail = "user@example.com";
        EmailVerificationEntity verification = verification(normalizedEmail, "otp-hash", EmailVerificationStatus.ACTIVE, 0,
                LocalDateTime.now().minusMinutes(1));
        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(emailVerificationRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE))
                .thenReturn(Optional.of(verification));
        when(emailVerificationRepository.save(any(EmailVerificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(VerificationCodeExpiredException.class,
                () -> registrationService.register(registerRequest(normalizedEmail, "some-password", "Nguyen Van A", "123456")));

        assertEquals(EmailVerificationStatus.EXPIRED, verification.getStatus());
        verify(emailVerificationRepository).save(verification);
        verify(userRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_firstInvalidOtpAttempt_shouldIncrementFailedAttempts() {
        assertInvalidOtpAttempt(0, 1, EmailVerificationStatus.ACTIVE);
    }

    @Test
    void register_secondInvalidOtpAttempt_shouldIncrementFailedAttempts() {
        assertInvalidOtpAttempt(1, 2, EmailVerificationStatus.ACTIVE);
    }

    @Test
    void register_thirdInvalidOtpAttempt_shouldInvalidateOtp() {
        assertInvalidOtpAttempt(2, 3, EmailVerificationStatus.INVALIDATED);
    }

    @Test
    void register_accountSaveFailure_shouldPropagateException() {
        String normalizedEmail = "user@example.com";
        EmailVerificationEntity verification = verification(normalizedEmail, "otp-hash", EmailVerificationStatus.ACTIVE, 0,
                LocalDateTime.now().plusMinutes(5));
        UserEntity savedUser = user(normalizedEmail, "encoded-password");

        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(emailVerificationRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true);
        when(passwordEncoder.encode("some-password")).thenReturn("encoded-password");
        when(accountNumberGenerator.generateUniqueAccountNumber()).thenReturn("ACC-123456789012");
        when(userRepository.save(any(UserEntity.class))).thenReturn(withId(savedUser, 1L));
        when(accountRepository.save(any(AccountEntity.class))).thenThrow(new RuntimeException("account save failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> registrationService.register(registerRequest(normalizedEmail, "some-password", "Nguyen Van A", "123456")));

        assertEquals("account save failed", ex.getMessage());
        verify(userRepository).save(any(UserEntity.class));
        verify(accountRepository).save(any(AccountEntity.class));
    }

    private void assertInvalidOtpAttempt(int initialFailedAttempts, int expectedFailedAttempts, EmailVerificationStatus expectedStatus) {
        String normalizedEmail = "user@example.com";
        EmailVerificationEntity verification = verification(normalizedEmail, "otp-hash", EmailVerificationStatus.ACTIVE, initialFailedAttempts,
                LocalDateTime.now().plusMinutes(5));

        when(userRepository.existsByEmailIgnoreCase(normalizedEmail)).thenReturn(false);
        when(emailVerificationRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(false);
        when(emailVerificationRepository.save(any(EmailVerificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(InvalidVerificationCodeException.class,
                () -> registrationService.register(registerRequest(normalizedEmail, "some-password", "Nguyen Van A", "123456")));

        assertEquals(expectedFailedAttempts, verification.getFailedAttempts());
        assertEquals(expectedStatus, verification.getStatus());
        verify(userRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(emailVerificationRepository).save(verification);
    }

    private RegisterRequest registerRequest(String email, String password, String fullName, String code) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFullName(fullName);
        request.setCode(code);
        return request;
    }

    private EmailVerificationEntity verification(String email, String codeHash, EmailVerificationStatus status,
            int failedAttempts, LocalDateTime expiresAt) {
        return new EmailVerificationEntity(email, codeHash, status, failedAttempts, expiresAt);
    }

    private UserEntity user(String email, String passwordHash) {
        return new UserEntity(email, passwordHash, "Nguyen Van A", UserRole.USER, UserStatus.ACTIVE);
    }

    private UserEntity withId(UserEntity user, Long id) {
        setId(user, id);
        return user;
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set entity id", ex);
        }
    }
}
