package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.ProfileResponse;
import com.chethu.paymentledgerservice.dto.UpdateProfileRequest;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.UserRepository;

class UserProfileServiceTest {
    @Test
    void getProfile_shouldMapSafeFieldsForAuthenticatedUser() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        UserProfileService service = new UserProfileService(repository);
        UserEntity user = user(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(user));

        ProfileResponse response = service.getProfile(42L);

        assertEquals(42L, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals(UserRole.USER, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertEquals(6, ProfileResponse.class.getDeclaredFields().length);
        verify(repository).findById(42L);
    }

    @Test
    void updateProfile_shouldChangeOnlyFullNameAndPersistCurrentUser() {
        UserRepository repository = mock(UserRepository.class);
        UserProfileService service = new UserProfileService(repository);
        UserEntity user = user(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("  Updated Name  ");

        ProfileResponse response = service.updateProfile(42L, request);

        assertEquals("Updated Name", user.getFullName());
        assertEquals("Updated Name", response.getFullName());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("hash", user.getPasswordHash());
        verify(repository).save(user);
    }

    @Test
    void getProfile_shouldRejectUnknownUser() {
        UserRepository repository = mock(UserRepository.class);
        UserProfileService service = new UserProfileService(repository);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getProfile(99L));
        verify(repository).findById(99L);
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity("user@example.com", "hash", "Nguyen Van A",
                UserRole.USER, UserStatus.ACTIVE);
        try {
            Field field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
