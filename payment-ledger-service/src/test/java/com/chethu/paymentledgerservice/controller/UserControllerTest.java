package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.ProfileResponse;
import com.chethu.paymentledgerservice.dto.UpdateProfileRequest;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.UserProfileService;

class UserControllerTest {
    @Test
    void getProfile_shouldPassPrincipalUserId() {
        UserProfileService service = mock(UserProfileService.class);
        UserController controller = new UserController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "User", null, null);
        ProfileResponse profile = mock(ProfileResponse.class);
        when(service.getProfile(42L)).thenReturn(profile);

        assertEquals(profile, controller.getProfile(principal).getBody());
        verify(service).getProfile(42L);
    }

    @Test
    void updateProfile_shouldPassOnlyPrincipalAndFocusedRequest() {
        UserProfileService service = mock(UserProfileService.class);
        UserController controller = new UserController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "User", null, null);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        when(service.updateProfile(eq(42L), eq(request))).thenReturn(mock(ProfileResponse.class));

        controller.updateProfile(principal, request);

        verify(service).updateProfile(42L, request);
    }

    @Test
    void profileEndpoints_shouldRejectMissingPrincipal() {
        UserController controller = new UserController(mock(UserProfileService.class));

        ResponseStatusException getException = assertThrows(ResponseStatusException.class,
                () -> controller.getProfile(null));
        ResponseStatusException putException = assertThrows(ResponseStatusException.class,
                () -> controller.updateProfile(null, new UpdateProfileRequest()));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), getException.getStatusCode().value());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), putException.getStatusCode().value());
    }

    @Test
    void updateRequest_shouldExposeOnlyFullName() throws Exception {
        assertEquals(1, UpdateProfileRequest.class.getDeclaredFields().length);
        Method setter = UpdateProfileRequest.class.getMethod("setFullName", String.class);
        assertEquals(void.class, setter.getReturnType());
    }
}
