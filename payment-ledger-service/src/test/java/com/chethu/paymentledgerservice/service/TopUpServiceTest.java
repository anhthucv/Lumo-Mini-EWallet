package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.dto.TopUpRequest;
import com.chethu.paymentledgerservice.dto.TopUpResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.exception.IdempotencyConflictException;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutRequest;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderException;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;

class TopUpServiceTest {
    @Test
    void createsPendingTopUpWithoutChangingWalletAndPersistsCheckout() {
        Fixture fixture = fixture();
        TopUpPaymentEntity payment = payment(fixture.account, 77L, new BigDecimal("100000.00"), "key-1");
        when(fixture.persistence.reserve(fixture.account, payment.getAmount(), "key-1")).thenReturn(payment);
        when(fixture.persistence.attachCheckout(77L, fixture.result)).thenReturn(paymentWithCheckout(payment));

        TopUpResponse response = fixture.service.createForCurrentUser(42L,
                new TopUpRequest(new BigDecimal("100000.00")), "key-1");

        ArgumentCaptor<PaymentCheckoutRequest> request = ArgumentCaptor.forClass(PaymentCheckoutRequest.class);
        verify(fixture.provider).createCheckout(request.capture());
        assertEquals(77L, request.getValue().merchantOrderCode());
        assertEquals(new BigDecimal("100000.00"), request.getValue().amount());
        assertEquals("VND", request.getValue().currency());
        assertEquals("https://payos.test/77", response.checkoutUrl());
        assertEquals(new BigDecimal("250000.00"), fixture.account.getBalance());
        verify(fixture.persistence).reserve(fixture.account, payment.getAmount(), "key-1");
        verify(fixture.persistence).attachCheckout(77L, fixture.result);
    }

    @Test
    void sameKeyAndAmountReplaysWithoutCallingProviderAgain() {
        Fixture fixture = fixture();
        TopUpPaymentEntity payment = payment(fixture.account, 77L, new BigDecimal("100000.00"), "key-1");
        paymentWithCheckout(payment);
        when(fixture.payments.findByAccountAndIdempotencyKey(fixture.account, "key-1"))
                .thenReturn(Optional.of(payment));

        TopUpResponse response = fixture.service.createForCurrentUser(42L,
                new TopUpRequest(new BigDecimal("100000.00")), "key-1");

        assertEquals("https://payos.test/77", response.checkoutUrl());
        verify(fixture.provider, never()).createCheckout(any());
        verify(fixture.persistence, never()).reserve(any(), any(), any());
    }

    @Test
    void sameKeyAndDifferentAmountConflicts() {
        Fixture fixture = fixture();
        TopUpPaymentEntity payment = payment(fixture.account, 77L, new BigDecimal("100000.00"), "key-1");
        when(fixture.payments.findByAccountAndIdempotencyKey(fixture.account, "key-1"))
                .thenReturn(Optional.of(payment));

        assertThrows(IdempotencyConflictException.class, () -> fixture.service.createForCurrentUser(42L,
                new TopUpRequest(new BigDecimal("200000.00")), "key-1"));
        verify(fixture.provider, never()).createCheckout(any());
    }

    @Test
    void providerFailureDoesNotCreditWallet() {
        Fixture fixture = fixture();
        TopUpPaymentEntity payment = payment(fixture.account, 77L, new BigDecimal("100000.00"), null);
        when(fixture.persistence.reserve(fixture.account, payment.getAmount(), null)).thenReturn(payment);
        when(fixture.provider.createCheckout(any())).thenThrow(new PaymentProviderException("provider unavailable"));

        assertThrows(PaymentProviderException.class, () -> fixture.service.createForCurrentUser(42L,
                new TopUpRequest(new BigDecimal("100000.00")), null));
        assertEquals(new BigDecimal("250000.00"), fixture.account.getBalance());
        verify(fixture.persistence, never()).attachCheckout(any(), any());
    }

    @Test
    void amountBelowTopUpMinimumIsRejectedBeforeProviderCall() {
        Fixture fixture = fixture();

        assertThrows(IllegalArgumentException.class, () -> fixture.service.createForCurrentUser(42L,
                new TopUpRequest(new BigDecimal("999.99")), null));
        verify(fixture.accountRepository, never()).findByUserId(any());
        verify(fixture.provider, never()).createCheckout(any());
    }

    private Fixture fixture() {
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TopUpPaymentRepository payments = Mockito.mock(TopUpPaymentRepository.class);
        TopUpPaymentPersistenceService persistence = Mockito.mock(TopUpPaymentPersistenceService.class);
        IdempotencyService idempotency = Mockito.mock(IdempotencyService.class);
        PaymentProvider provider = Mockito.mock(PaymentProvider.class);
        AccountEntity account = new AccountEntity("ACC-TOPUP", "Top-up Owner");
        account.deposit(new BigDecimal("250000.00"));
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(idempotency.normalizeKey(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentCheckoutResult result = new PaymentCheckoutResult(PaymentProviderType.PAYOS, 77L,
                "payos-ref-77", "https://payos.test/77");
        when(provider.createCheckout(any())).thenReturn(result);
        return new Fixture(accounts, payments, persistence, provider, account, result,
                new TopUpService(accounts, payments, persistence, idempotency, provider));
    }

    private TopUpPaymentEntity payment(AccountEntity account, Long id, BigDecimal amount, String key) {
        TopUpPaymentEntity payment = new TopUpPaymentEntity(account, amount, key);
        setField(payment, "id", id);
        payment.assignMerchantOrderCode();
        return payment;
    }

    private TopUpPaymentEntity paymentWithCheckout(TopUpPaymentEntity payment) {
        payment.attachCheckout(fixtureResult(payment.getMerchantOrderCode()));
        return payment;
    }

    private PaymentCheckoutResult fixtureResult(long orderCode) {
        return new PaymentCheckoutResult(PaymentProviderType.PAYOS, orderCode, "payos-ref-" + orderCode,
                "https://payos.test/" + orderCode);
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private record Fixture(AccountRepository accountRepository, TopUpPaymentRepository payments,
            TopUpPaymentPersistenceService persistence, PaymentProvider provider, AccountEntity account,
            PaymentCheckoutResult result, TopUpService service) {
    }
}
