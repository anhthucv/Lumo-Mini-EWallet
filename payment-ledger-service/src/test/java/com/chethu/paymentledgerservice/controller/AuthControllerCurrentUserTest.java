package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.CurrentUserResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;

class AuthControllerCurrentUserTest {
    private final AuthController controller = new AuthController(null, null, null);

    @Test
    void me_shouldReturnSafeCurrentUser_fromAuthenticatedPrincipal() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L,
                "user@example.com",
                "Nguyen Van A",
                UserRole.USER,
                UserStatus.ACTIVE);

        CurrentUserResponse response = controller.me(principal, null).getBody();

        assertNotNull(response);
        assertEquals(42L, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals(UserRole.USER, response.getRole());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
    }

    @Test
    void currentUserResponse_shouldNotExposePasswordHashField() {
        boolean exposesPasswordHash = Arrays.stream(CurrentUserResponse.class.getDeclaredFields())
                .anyMatch(field -> field.getName().equals("passwordHash"));

        assertTrue(!exposesPasswordHash, "CurrentUserResponse must not expose passwordHash");
    }

    @Test
    void me_shouldReturnUnauthorized_whenPrincipalMissing() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.me(null, null));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
    }

    @Test
    void me_shouldReturnUnauthorized_whenAuthenticationHasUnexpectedPrincipalType() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("unexpected-principal", null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.me(null, authentication));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
    }

    @Test
    void me_shouldNotDeclareAClientSuppliedUserIdParameter() throws Exception {
        Method method = AuthController.class.getMethod(
                "me",
                AuthenticatedUserPrincipal.class,
                Authentication.class);

        assertEquals(2, method.getParameterCount());
        assertEquals(AuthenticatedUserPrincipal.class, method.getParameterTypes()[0]);
        assertEquals(Authentication.class, method.getParameterTypes()[1]);
    }
}
