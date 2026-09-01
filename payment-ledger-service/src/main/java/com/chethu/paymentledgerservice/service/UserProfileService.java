package com.chethu.paymentledgerservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.dto.ProfileResponse;
import com.chethu.paymentledgerservice.dto.UpdateProfileRequest;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class UserProfileService {
    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ProfileResponse getProfile(Long userId) {
        return ProfileResponse.from(findUser(userId));
    }

    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = findUser(userId);
        user.changeFullName(request.getFullName().trim());
        return ProfileResponse.from(userRepository.save(user));
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
