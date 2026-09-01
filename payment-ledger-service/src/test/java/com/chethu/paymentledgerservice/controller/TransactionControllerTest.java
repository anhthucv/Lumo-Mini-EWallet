package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.chethu.paymentledgerservice.dto.TransactionResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.TransactionService;

class TransactionControllerTest {
    @Test
    void getHistory_shouldUseAuthenticatedUserAndDefaultPagination() {
        TransactionService service = mock(TransactionService.class);
        TransactionController controller = new TransactionController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "User", null, null);
        Page<TransactionResponse> history = new PageImpl<>(List.of());
        when(service.getHistoryForUser(eq(42L), any(Pageable.class))).thenReturn(history);

        Page<TransactionResponse> response = controller.getHistory(principal, 0, 10, "createdAt,desc").getBody();

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        verify(service).getHistoryForUser(eq(42L), any(Pageable.class));
    }

    @Test
    void getHistory_shouldRejectClientAccountIdByMethodShape() throws Exception {
        assertEquals(4, TransactionController.class.getMethod(
                "getHistory", AuthenticatedUserPrincipal.class, int.class, int.class, String.class)
                .getParameterCount());
    }
}
