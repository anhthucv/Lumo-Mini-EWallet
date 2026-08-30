package com.chethu.paymentledgerservice.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.LoginRequest;
import com.chethu.paymentledgerservice.dto.LoginResponse;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.InvalidCredentialsException;
import com.chethu.paymentledgerservice.exception.UserLockedException;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(()-> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new UserLockedException();
        }

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
