package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.LoginRequest;
import com.chethu.paymentledgerservice.dto.LoginResponse;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.InvalidCredentialsException;
import com.chethu.paymentledgerservice.exception.UserLockedException;
import com.chethu.paymentledgerservice.repository.UserRepository;
import com.chethu.paymentledgerservice.security.JwtService;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void login_shouldThrowInvalidCredentials_whenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(loginRequest("user@example.com", "secret-password")));

        verify(userRepository).findByEmailIgnoreCase("user@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldThrowInvalidCredentials_whenPasswordDoesNotMatch() {
        UserEntity user = user(11L, "user@example.com", "stored-hash", UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> loginService.login(loginRequest("user@example.com", "wrong-password")));

        verify(userRepository).findByEmailIgnoreCase("user@example.com");
        verify(passwordEncoder).matches("wrong-password", "stored-hash");
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldThrowUserLocked_whenPasswordMatchesButUserIsLocked() {
        UserEntity user = user(12L, "user@example.com", "stored-hash", UserStatus.LOCKED);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret-password", "stored-hash")).thenReturn(true);

        assertThrows(UserLockedException.class,
                () -> loginService.login(loginRequest("user@example.com", "secret-password")));

        verify(userRepository).findByEmailIgnoreCase("user@example.com");
        verify(passwordEncoder).matches("secret-password", "stored-hash");
        verify(jwtService, never()).generateAccessToken(any(UserEntity.class));
    }

    @Test
    void login_shouldReturnSafeUserData_forActiveUser() throws Exception {
        UserEntity user = user(13L, "user@example.com", "stored-hash", UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret-password", "stored-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = loginService.login(loginRequest("user@example.com", "secret-password"));

        assertEquals(13L, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals(UserRole.USER, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600000L, response.getExpiresIn());
        assertPasswordHashNotExposed(response);

        verify(userRepository).findByEmailIgnoreCase("user@example.com");
        verify(passwordEncoder).matches("secret-password", "stored-hash");
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).getExpirationMs();
    }

    @Test
    void login_shouldNormalizeEmailBeforeRepositoryLookup() {
        UserEntity user = user(14L, "user@example.com", "stored-hash", UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret-password", "stored-hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        loginService.login(loginRequest("  USER@Example.com  ", "secret-password"));

        verify(userRepository).findByEmailIgnoreCase("user@example.com");
        verify(jwtService).generateAccessToken(user);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private UserEntity user(Long id, String email, String passwordHash, UserStatus status) {
        UserEntity user = new UserEntity(email, passwordHash, "Nguyen Van A", UserRole.USER, status);
        setField(user, "id", id);
        return user;
    }

    private void setField(UserEntity user, String fieldName, Object value) {
        try {
            Field field = UserEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(user, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set field " + fieldName, ex);
        }
    }

    private void assertPasswordHashNotExposed(LoginResponse response) throws Exception {
        Method[] methods = LoginResponse.class.getMethods();
        for (Method method : methods) {
            String methodName = method.getName().toLowerCase(Locale.ROOT);
            assertTrue(!methodName.contains("passwordhash"), "LoginResponse must not expose password hash accessors");
        }
        assertThrows(NoSuchMethodException.class, () -> LoginResponse.class.getMethod("getPasswordHash"));
    }
}
