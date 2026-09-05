package com.chethu.paymentledgerservice.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.LoginRequest;
import com.chethu.paymentledgerservice.dto.LoginResponse;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.InvalidCredentialsException;
import com.chethu.paymentledgerservice.exception.UserLockedException;
import com.chethu.paymentledgerservice.security.JwtService;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(()-> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("login rejected for locked userId={}", user.getId());
            throw new UserLockedException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        log.info("login succeeded userId={} role={}", user.getId(), user.getRole());
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                accessToken,
                "Bearer",
                jwtService.getExpirationMs());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
