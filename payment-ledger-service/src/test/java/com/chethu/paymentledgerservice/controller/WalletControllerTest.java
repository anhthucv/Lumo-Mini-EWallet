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
import com.chethu.paymentledgerservice.dto.AccountResponse;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.RecipientResponse;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.dto.TransactionLimitResponse;
import com.chethu.paymentledgerservice.dto.WalletLimitsResponse;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;
import com.chethu.paymentledgerservice.repository.IdempotencyRecordRepository;
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

    @Test
    void limits_shouldReturnAuthenticatedUsersWalletLimits() {
        StubAccountService service = new StubAccountService();
        WalletController walletController = new WalletController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "Nguyen Van A", null, null);

        WalletLimitsResponse response = walletController.limits(principal).getBody();

        assertNotNull(response);
        assertEquals(new BigDecimal("50000000.00"), response.deposit().perTransactionLimit());
        assertEquals(new BigDecimal("80000000.00"), response.deposit().remainingToday());
    }

    @Test
    void limits_shouldRequireAuthenticatedPrincipal() {
        assertThrows(ResponseStatusException.class, () -> controller.limits(null));
    }

    @Test
    void deposit_shouldPassPrincipalUserIdAndAmountToService() {
        StubAccountService service = new StubAccountService();
        WalletController walletController = new WalletController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "Nguyen Van A", null, null);
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("100000.00"));

        AccountResponse response = walletController.deposit(principal, null, request).getBody();

        assertNotNull(response);
        assertEquals(42L, service.depositUserId);
        assertEquals(new BigDecimal("100000.00"), service.depositAmount);
    }

    @Test
    void deposit_shouldNotDeclareClientAccountOrUserId() throws Exception {
        Method method = WalletController.class.getMethod(
                "deposit", AuthenticatedUserPrincipal.class, String.class, MoneyOperationRequest.class);

        assertEquals(3, method.getParameterCount());
        assertEquals(AuthenticatedUserPrincipal.class, method.getParameterTypes()[0]);
        assertEquals(String.class, method.getParameterTypes()[1]);
        assertEquals(MoneyOperationRequest.class, method.getParameterTypes()[2]);
    }

    @Test
    void getRecipient_shouldDelegateAccountNumberToService() {
        StubAccountService service = new StubAccountService();
        WalletController walletController = new WalletController(service);

        RecipientResponse response = walletController.getRecipient("ACC-123456789012").getBody();

        assertNotNull(response);
        assertEquals("ACC-123456789012", service.recipientAccountNumber);
        assertEquals("ACC-123456789012", response.getAccountNumber());
        assertEquals("Nguyen Van B", response.getOwnerName());
    }

    @Test
    void getRecipient_shouldExposeOnlyAccountNumberAndOwnerName() throws Exception {
        assertEquals(2, RecipientResponse.class.getDeclaredFields().length);
        assertThrows(NoSuchMethodException.class, () -> RecipientResponse.class.getMethod("getId"));
        assertThrows(NoSuchMethodException.class, () -> RecipientResponse.class.getMethod("getBalance"));
        assertThrows(NoSuchMethodException.class, () -> RecipientResponse.class.getMethod("getUserId"));
    }

    @Test
    void transfer_shouldPassPrincipalUserIdAndRequestToService() {
        StubAccountService service = new StubAccountService();
        WalletController walletController = new WalletController(service);
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                42L, "user@example.com", "Sender", null, null);
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber("ACC-RECIPIENT");
        request.setAmount(new BigDecimal("100000.00"));

        AccountResponse response = walletController.transfer(principal, null, request).getBody();

        assertNotNull(response);
        assertEquals(42L, service.transferUserId);
        assertEquals("ACC-RECIPIENT", service.transferRecipient);
        assertEquals(new BigDecimal("100000.00"), service.transferAmount);
    }

    @Test
    void transfer_shouldNotDeclareClientSenderIdentifiers() throws Exception {
        Method method = WalletController.class.getMethod(
                "transfer", AuthenticatedUserPrincipal.class, String.class, TransferRequest.class);

        assertEquals(3, method.getParameterCount());
        assertEquals(AuthenticatedUserPrincipal.class, method.getParameterTypes()[0]);
        assertEquals(String.class, method.getParameterTypes()[1]);
        assertEquals(TransferRequest.class, method.getParameterTypes()[2]);
        assertThrows(NoSuchMethodException.class,
                () -> TransferRequest.class.getMethod("getFromAccountId"));
        assertThrows(NoSuchMethodException.class,
                () -> TransferRequest.class.getMethod("getToAccountId"));
    }

    private static final class StubAccountService extends AccountService {
        private Long depositUserId;
        private BigDecimal depositAmount;
        private String recipientAccountNumber;
        private Long transferUserId;
        private String transferRecipient;
        private BigDecimal transferAmount;

        StubAccountService() {
            super(null, null, null,
                    org.mockito.Mockito.mock(LedgerAccountRepository.class),
                    org.mockito.Mockito.mock(JournalRepository.class),
                    new com.chethu.paymentledgerservice.service.IdempotencyService(
                            org.mockito.Mockito.mock(IdempotencyRecordRepository.class)),
                    org.mockito.Mockito.mock(com.chethu.paymentledgerservice.service.TransactionLimitService.class),
                    org.mockito.Mockito.mock(com.chethu.paymentledgerservice.service.RiskEvaluationService.class,
                            invocation -> new com.chethu.paymentledgerservice.service.RiskEvaluationResult(
                                    com.chethu.paymentledgerservice.domain.RiskDecision.ALLOW, java.util.List.of())),
                    org.mockito.Mockito.mock(com.chethu.paymentledgerservice.service.RiskAuditService.class));
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

        @Override
        public WalletLimitsResponse getWalletLimits(Long userId) {
            TransactionLimitResponse deposit = new TransactionLimitResponse(
                    new BigDecimal("50000000.00"), new BigDecimal("100000000.00"),
                    new BigDecimal("20000000.00"), new BigDecimal("80000000.00"));
            TransactionLimitResponse withdraw = new TransactionLimitResponse(
                    new BigDecimal("20000000.00"), new BigDecimal("50000000.00"),
                    BigDecimal.ZERO, new BigDecimal("50000000.00"));
            TransactionLimitResponse transfer = new TransactionLimitResponse(
                    new BigDecimal("50000000.00"), new BigDecimal("100000000.00"),
                    BigDecimal.ZERO, new BigDecimal("100000000.00"));
            return new WalletLimitsResponse(deposit, withdraw, transfer);
        }

        @Override
        public AccountResponse depositForCurrentUser(Long userId, MoneyOperationRequest request, String idempotencyKey) {
            depositUserId = userId;
            depositAmount = request.getAmount();
            return new AccountResponse(100L, "ACC-123456789012", "Nguyen Van A",
                    request.getAmount(), AccountStatus.ACTIVE);
        }

        @Override
        public RecipientResponse getRecipient(String accountNumber) {
            recipientAccountNumber = accountNumber;
            return new RecipientResponse(accountNumber, "Nguyen Van B");
        }

        @Override
        public AccountResponse transferForCurrentUser(Long userId, TransferRequest request, String idempotencyKey) {
            transferUserId = userId;
            transferRecipient = request.getRecipientAccountNumber();
            transferAmount = request.getAmount();
            return new AccountResponse(100L, "ACC-SENDER", "Sender", request.getAmount(), AccountStatus.ACTIVE);
        }
    }
}
