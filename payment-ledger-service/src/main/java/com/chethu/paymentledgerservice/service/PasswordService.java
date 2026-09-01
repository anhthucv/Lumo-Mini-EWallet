package com.chethu.paymentledgerservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.dto.ChangePasswordRequest;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.InvalidCurrentPasswordException;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class PasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.changePasswordHash(newPasswordHash);
        userRepository.save(user);
    }
}
