package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.ChangePasswordRequest;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.PasswordService;

class UserPasswordControllerTest {
    @Test
    void changePassword_shouldPassPrincipalUserIdAndReturnNoContent() {
        PasswordService service = mock(PasswordService.class);
        UserPasswordController controller = new UserPasswordController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "User", null, null);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current-secret");
        request.setNewPassword("new-secret");

        assertEquals(HttpStatus.NO_CONTENT, controller.changePassword(principal, request).getStatusCode());
        verify(service).changePassword(42L, request);
    }

    @Test
    void changePassword_shouldRejectMissingPrincipal() {
        UserPasswordController controller = new UserPasswordController(mock(PasswordService.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.changePassword(null, new ChangePasswordRequest()));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
    }

    @Test
    void request_shouldContainOnlyCurrentAndNewPasswordFields() throws Exception {
        assertEquals(2, ChangePasswordRequest.class.getDeclaredFields().length);
        Method userIdSetter = null;
        for (Method method : ChangePasswordRequest.class.getMethods()) {
            if (method.getName().toLowerCase().contains("userid")) {
                userIdSetter = method;
            }
        }
        assertEquals(null, userIdSetter);
    }
}
