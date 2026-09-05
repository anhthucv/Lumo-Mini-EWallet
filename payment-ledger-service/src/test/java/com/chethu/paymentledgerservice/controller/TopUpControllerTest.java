package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.dto.TopUpRequest;
import com.chethu.paymentledgerservice.dto.TopUpResponse;
import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.TopUpService;

class TopUpControllerTest {
    @Test
    void usesAuthenticatedPrincipalAndReturnsCreated() {
        TopUpService service = mock(TopUpService.class);
        TopUpController controller = new TopUpController(service);
        TopUpRequest request = new TopUpRequest(new BigDecimal("100000.00"));
        TopUpResponse expected = new TopUpResponse(1L, 1L, request.amount(), "VND", TopUpPaymentStatus.PENDING,
                PaymentProviderType.PAYOS, "https://payos.test/1", null);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(42L, "a@b.test", "Owner", null, null);
        org.mockito.Mockito.when(service.createForCurrentUser(42L, request, "key-1")).thenReturn(expected);

        var response = controller.create(principal, "key-1", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(service).createForCurrentUser(eq(42L), eq(request), eq("key-1"));
    }

    @Test
    void rejectsMissingPrincipal() {
        TopUpController controller = new TopUpController(mock(TopUpService.class));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.create(null, null, new TopUpRequest(new BigDecimal("100000.00"))));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
