package com.chethu.paymentledgerservice.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.EmailVerificationStatus;
import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.entity.EmailVerificationEntity;
import com.chethu.paymentledgerservice.exception.DuplicateEmailException;
import com.chethu.paymentledgerservice.exception.VerificationCodeResendTooSoonException;
import com.chethu.paymentledgerservice.repository.EmailVerificationRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class EmailVerificationService {
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public EmailVerificationService(UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            VerificationCodeGenerator verificationCodeGenerator,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public VerificationCodeResponse sendRegistrationCode(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail)
                .ifPresent(latest -> {
                    if (Duration.between(latest.getCreatedAt(), now).compareTo(RESEND_COOLDOWN) < 0) {
                        throw new VerificationCodeResendTooSoonException();
                    }
                });

        emailVerificationRepository.findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(normalizedEmail,
                EmailVerificationStatus.ACTIVE)
                .ifPresent(activeVerification -> {
                    activeVerification.invalidate();
                    emailVerificationRepository.save(activeVerification);
                });

        String rawCode = verificationCodeGenerator.generateCode();
        String codeHash = passwordEncoder.encode(rawCode);
        EmailVerificationEntity verification = new EmailVerificationEntity(
                normalizedEmail,
                codeHash,
                EmailVerificationStatus.ACTIVE,
                0,
                now.plus(CODE_EXPIRATION));
        emailVerificationRepository.save(verification);
        emailService.sendVerificationCode(normalizedEmail, rawCode);
        return new VerificationCodeResponse("Verification code sent");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
