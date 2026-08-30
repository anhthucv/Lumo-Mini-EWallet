package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class RegistrationService {
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountNumberGenerator accountNumberGenerator;

    public RegistrationService(UserRepository userRepository, AccountRepository accountRepository,
            EmailVerificationRepository emailVerificationRepository, PasswordEncoder passwordEncoder,
            AccountNumberGenerator accountNumberGenerator) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Transactional(noRollbackFor = {
            VerificationCodeExpiredException.class,
            InvalidVerificationCodeException.class
    })
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        EmailVerificationEntity verification = emailVerificationRepository
                .findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail, EmailVerificationStatus.ACTIVE)
                .orElseThrow(() -> new VerificationCodeNotFoundException(normalizedEmail));

        LocalDateTime now = LocalDateTime.now();
        if (verification.getExpiresAt().isBefore(now)) {
            verification.markExpired();
            emailVerificationRepository.save(verification);
            throw new VerificationCodeExpiredException();
        }

        if (!passwordEncoder.matches(request.getCode(), verification.getCodeHash())) {
            verification.incrementFailedAttempts();
            if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                verification.invalidate();
            }
            emailVerificationRepository.save(verification);
            throw new InvalidVerificationCodeException();
        }

        UserEntity savedUser = userRepository.save(new UserEntity(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                UserRole.USER,
                UserStatus.ACTIVE));

        AccountEntity account = new AccountEntity(accountNumberGenerator.generateUniqueAccountNumber(),
                savedUser.getFullName());
        account.assignUser(savedUser);
        AccountEntity savedAccount = accountRepository.save(account);

        verification.markUsed();
        emailVerificationRepository.save(verification);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedAccount.getId(),
                savedAccount.getAccountNumber(),
                savedAccount.getBalance(),
                savedUser.getRole(),
                savedUser.getStatus(),
                savedAccount.getStatus());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
