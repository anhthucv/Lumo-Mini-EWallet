package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.dto.MyWalletResponse;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.AccountService;

class WalletControllerTest {
    private final WalletController controller = new WalletController(new StubAccountService());

    @Test
    void me_shouldReturnAuthenticatedUsersWallet() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L,
                "user@example.com",
                "Nguyen Van A",
                null,
                null);

        MyWalletResponse response = controller.me(principal).getBody();

        assertNotNull(response);
        assertEquals(100L, response.getAccountId());
        assertEquals("ACC-123456789012", response.getAccountNumber());
        assertEquals("Nguyen Van A", response.getOwnerName());
        assertEquals(new BigDecimal("250000.00"), response.getBalance());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
    }

    @Test
    void me_shouldReturnUnauthorized_whenPrincipalMissing() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.me(null));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exception.getStatusCode().value());
    }

    @Test
    void me_shouldUseAuthenticatedPrincipalAndNotAcceptClientUserId() throws Exception {
        Method method = WalletController.class.getMethod("me", AuthenticatedUserPrincipal.class);

        assertEquals(1, method.getParameterCount());
        assertEquals(AuthenticatedUserPrincipal.class, method.getParameterTypes()[0]);
    }

    private static final class StubAccountService extends AccountService {
        StubAccountService() {
            super(null, null, null);
        }

        @Override
        public MyWalletResponse getMyWallet(Long userId) {
            return new MyWalletResponse(
                    100L,
                    "ACC-123456789012",
                    "Nguyen Van A",
                    new BigDecimal("250000.00"),
                    AccountStatus.ACTIVE);
        }
    }
}
