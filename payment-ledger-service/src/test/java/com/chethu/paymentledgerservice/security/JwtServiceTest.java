package com.chethu.paymentledgerservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.entity.UserEntity;

class JwtServiceTest {
    private static final String SECRET = "payment-ledger-test-jwt-secret-change-me-to-a-long-enough-value";

    @Test
    void generateToken_shouldContainExpectedClaims() {
        JwtService jwtService = new JwtService(SECRET, 3600000L);
        UserEntity user = user(7L, "user@example.com", UserRole.USER, UserStatus.ACTIVE);

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertEquals("user@example.com", jwtService.extractSubject(token));
        assertEquals(7L, jwtService.extractUserId(token));
        assertEquals(UserRole.USER, jwtService.extractRole(token));
        assertTrue(jwtService.isTokenSignatureAndStructureValid(token));
        assertFalse(jwtService.isTokenExpired(token));
        assertTrue(jwtService.belongsToUser(token, user));
        assertNotNull(jwtService.extractExpiration(token));
        assertTrue(jwtService.extractExpiration(token).isAfter(Instant.now()));
    }

    @Test
    void expiredToken_shouldBeRejected() {
        JwtService jwtService = new JwtService(SECRET, -1000L);
        UserEntity user = user(8L, "user@example.com", UserRole.USER, UserStatus.ACTIVE);

        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenSignatureAndStructureValid(token));
        assertTrue(jwtService.isTokenExpired(token));
        assertFalse(jwtService.belongsToUser(token, user));
    }

    @Test
    void tamperedToken_shouldFailSignatureValidation() {
        JwtService jwtService = new JwtService(SECRET, 3600000L);
        UserEntity user = user(9L, "user@example.com", UserRole.USER, UserStatus.ACTIVE);
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertFalse(jwtService.isTokenSignatureAndStructureValid(tampered));
        assertFalse(jwtService.belongsToUser(tampered, user));
    }

    private UserEntity user(Long id, String email, UserRole role, UserStatus status) {
        UserEntity user = new UserEntity(email, "hash", "Nguyen Van A", role, status);
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
