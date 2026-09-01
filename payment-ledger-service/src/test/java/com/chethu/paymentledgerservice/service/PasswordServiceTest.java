package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.ChangePasswordRequest;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.InvalidCurrentPasswordException;
import com.chethu.paymentledgerservice.repository.UserRepository;

class PasswordServiceTest {
    @Test
    void changePassword_shouldVerifyAndPersistOnlyEncodedNewPassword() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordService service = new PasswordService(repository, encoder);
        UserEntity user = user(42L, "old-hash");
        when(repository.findById(42L)).thenReturn(Optional.of(user));
        when(encoder.matches("current-secret", "old-hash")).thenReturn(true);
        when(encoder.encode("new-secret")).thenReturn("new-hash");

        service.changePassword(42L, request("current-secret", "new-secret"));

        verify(encoder).matches("current-secret", "old-hash");
        verify(encoder).encode("new-secret");
        verify(repository).save(user);
        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void changePassword_shouldRejectWrongCurrentPasswordWithoutEncodingOrSaving() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordService service = new PasswordService(repository, encoder);
        UserEntity user = user(42L, "old-hash");
        when(repository.findById(42L)).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-secret", "old-hash")).thenReturn(false);

        assertThrows(InvalidCurrentPasswordException.class,
                () -> service.changePassword(42L, request("wrong-secret", "new-secret")));

        verify(encoder, never()).encode(any());
        verify(repository, never()).save(any());
        assertEquals("old-hash", user.getPasswordHash());
    }

    @Test
    void changePassword_shouldUseAuthenticatedUserIdLookup() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordService service = new PasswordService(repository, encoder);
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(com.chethu.paymentledgerservice.exception.UserNotFoundException.class,
                () -> service.changePassword(42L, request("current-secret", "new-secret")));
        verify(repository).findById(42L);
    }

    private ChangePasswordRequest request(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private UserEntity user(Long id, String passwordHash) {
        UserEntity user = new UserEntity("user@example.com", passwordHash, "User", UserRole.USER, UserStatus.ACTIVE);
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
