package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;
import com.chethu.paymentledgerservice.payment.provider.ProviderPaymentStatus;
import com.chethu.paymentledgerservice.payment.provider.ProviderPaymentStatusResult;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;

class TopUpStatusSyncServiceTest {
    @Test
    void pendingPayment_shouldQueryProviderAndDelegateFinalization() {
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TopUpPaymentRepository payments = Mockito.mock(TopUpPaymentRepository.class);
        PaymentProvider provider = Mockito.mock(PaymentProvider.class);
        TopUpFinalizationService finalization = Mockito.mock(TopUpFinalizationService.class);
        AccountEntity account = new AccountEntity("ACC-SYNC", "Owner");
        TopUpPaymentEntity payment = payment(account, 11L, 11L, TopUpPaymentStatus.PENDING);
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(payments.findByIdAndAccount(11L, account)).thenReturn(Optional.of(payment));
        when(provider.getPaymentStatus(11L)).thenReturn(new ProviderPaymentStatusResult(PaymentProviderType.PAYOS,
                11L, new BigDecimal("1000.00"), "VND", "provider-ref", "provider-tx", ProviderPaymentStatus.PENDING));

        new TopUpStatusSyncService(accounts, payments, provider, finalization).syncForCurrentUser(42L, 11L);

        verify(provider).getPaymentStatus(11L);
        verify(finalization).applyProviderStatus(any());
    }

    @Test
    void completedPayment_shouldReturnPersistedStateWithoutProviderCall() {
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TopUpPaymentRepository payments = Mockito.mock(TopUpPaymentRepository.class);
        PaymentProvider provider = Mockito.mock(PaymentProvider.class);
        AccountEntity account = new AccountEntity("ACC-SYNC", "Owner");
        TopUpPaymentEntity payment = payment(account, 11L, 11L, TopUpPaymentStatus.SUCCESS);
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(payments.findByIdAndAccount(11L, account)).thenReturn(Optional.of(payment));

        new TopUpStatusSyncService(accounts, payments, provider, Mockito.mock(TopUpFinalizationService.class))
                .syncForCurrentUser(42L, 11L);

        verify(provider, never()).getPaymentStatus(anyLong());
    }

    @Test
    void missingCheckoutOrder_shouldFailSafelyWithoutProviderCall() {
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TopUpPaymentRepository payments = Mockito.mock(TopUpPaymentRepository.class);
        PaymentProvider provider = Mockito.mock(PaymentProvider.class);
        AccountEntity account = new AccountEntity("ACC-SYNC", "Owner");
        TopUpPaymentEntity payment = payment(account, 11L, null, TopUpPaymentStatus.PENDING);
        when(accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(payments.findByIdAndAccount(11L, account)).thenReturn(Optional.of(payment));

        assertThrows(com.chethu.paymentledgerservice.payment.provider.PaymentProviderException.class,
                () -> new TopUpStatusSyncService(accounts, payments, provider, Mockito.mock(TopUpFinalizationService.class))
                        .syncForCurrentUser(42L, 11L));
        verify(provider, never()).getPaymentStatus(anyLong());
    }

    @Test
    void unknownWallet_shouldBeRejectedBeforePaymentLookup() {
        AccountRepository accounts = Mockito.mock(AccountRepository.class);
        TopUpPaymentRepository payments = Mockito.mock(TopUpPaymentRepository.class);
        when(accounts.findByUserId(42L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> new TopUpStatusSyncService(accounts, payments,
                Mockito.mock(PaymentProvider.class), Mockito.mock(TopUpFinalizationService.class))
                .syncForCurrentUser(42L, 11L));
        verify(payments, never()).findByIdAndAccount(any(), any());
    }

    private TopUpPaymentEntity payment(AccountEntity account, Long id, Long orderCode, TopUpPaymentStatus status) {
        TopUpPaymentEntity payment = new TopUpPaymentEntity(account, new BigDecimal("1000.00"), "key");
        setField(payment, "id", id);
        setField(payment, "merchantOrderCode", orderCode);
        setField(payment, "status", status);
        return payment;
    }

    private void setField(Object target, String name, Object value) {
        try { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value); }
        catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
}
