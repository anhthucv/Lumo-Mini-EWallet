package com.chethu.paymentledgerservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.MarkAllNotificationsReadResponse;
import com.chethu.paymentledgerservice.dto.NotificationResponse;
import com.chethu.paymentledgerservice.dto.UnreadCountResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.NotificationPersistenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Authenticated financial notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationPersistenceService notificationPersistenceService;

    public NotificationController(NotificationPersistenceService notificationPersistenceService) {
        this.notificationPersistenceService = notificationPersistenceService;
    }

    @GetMapping
    @Operation(summary = "List my notifications")
    public ResponseEntity<Page<NotificationResponse>> findAll(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requirePrincipal(principal);
        validatePagination(page, size);
        return ResponseEntity.ok(notificationPersistenceService.findForUser(
                principal.userId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")))));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get my unread notification count")
    public ResponseEntity<UnreadCountResponse> unreadCount(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePrincipal(principal);
        return ResponseEntity.ok(notificationPersistenceService.unreadCountForUser(principal.userId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<NotificationResponse> markRead(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id) {
        requirePrincipal(principal);
        return ResponseEntity.ok(notificationPersistenceService.markReadForUser(principal.userId(), id));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<MarkAllNotificationsReadResponse> markAllRead(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePrincipal(principal);
        return ResponseEntity.ok(notificationPersistenceService.markAllReadForUser(principal.userId()));
    }

    private void requirePrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination values");
        }
    }
}
