package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.MarkAllNotificationsReadResponse;
import com.chethu.paymentledgerservice.dto.NotificationResponse;
import com.chethu.paymentledgerservice.dto.UnreadCountResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.NotificationPersistenceService;

class NotificationControllerTest {
    private final NotificationPersistenceService service = mock(NotificationPersistenceService.class);
    private final NotificationController controller = new NotificationController(service);
    private final AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
            42L, "user@example.com", "User", null, null);

    @Test
    void listUsesDefaultPageSizeAndOwnerFromPrincipal() {
        Page<NotificationResponse> page = new PageImpl<>(List.of());
        when(service.findForUser(eq(42L), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<NotificationResponse>> response = controller.findAll(principal, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        verify(service).findForUser(eq(42L), any(Pageable.class));
    }

    @Test
    void readOperationsUseCurrentUserAndDedicatedResponses() {
        NotificationResponse notification = new NotificationResponse(
                7L, null, "Title", "Message", null, "REF-1", true, null, null);
        when(service.unreadCountForUser(42L)).thenReturn(new UnreadCountResponse(3));
        when(service.markReadForUser(42L, 7L)).thenReturn(notification);
        when(service.markAllReadForUser(42L)).thenReturn(new MarkAllNotificationsReadResponse(2));

        assertEquals(3, controller.unreadCount(principal).getBody().unreadCount());
        assertEquals(notification, controller.markRead(principal, 7L).getBody());
        assertEquals(2, controller.markAllRead(principal).getBody().updatedCount());
        verify(service).markReadForUser(42L, 7L);
        verify(service).markAllReadForUser(42L);
    }

    @Test
    void unauthenticatedAndInvalidPaginationRequestsAreRejected() {
        ResponseStatusException unauthorized = assertThrows(ResponseStatusException.class,
                () -> controller.unreadCount(null));
        ResponseStatusException invalidPage = assertThrows(ResponseStatusException.class,
                () -> controller.findAll(principal, -1, 20));
        ResponseStatusException invalidSize = assertThrows(ResponseStatusException.class,
                () -> controller.findAll(principal, 0, 101));

        assertEquals(HttpStatus.UNAUTHORIZED, unauthorized.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidPage.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, invalidSize.getStatusCode());
    }
}
