package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.chethu.paymentledgerservice.domain.PayoutProviderType;
import com.chethu.paymentledgerservice.domain.PayoutStatus;
import com.chethu.paymentledgerservice.dto.PayoutRequest;
import com.chethu.paymentledgerservice.dto.PayoutResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.PayoutEntity;
import com.chethu.paymentledgerservice.payment.payout.PayoutProvider;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutRequest;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutResult;
import com.chethu.paymentledgerservice.repository.PayoutRepository;

class PayoutServiceTest {
    @Test
    void sendsFullDecryptedAccountToProviderAndReturnsPendingPayout() {
        Fixture fixture = fixture();
        PayoutRequest request = new PayoutRequest(new BigDecimal("30000.00"), "970422", "0123456789");

        PayoutResponse response = fixture.service.createForCurrentUser(42L, request, "key-1");

        ArgumentCaptor<ProviderPayoutRequest> captor = ArgumentCaptor.forClass(ProviderPayoutRequest.class);
        verify(fixture.provider).createPayout(captor.capture());
        assertEquals("0123456789", captor.getValue().destinationAccountNumber());
        assertEquals(PayoutStatus.PENDING, response.status());
        assertEquals("****6789", response.destinationAccountSummary());
    }

    @Test
    void alreadyStartedPayoutDoesNotCallProviderAgain() {
        Fixture fixture = fixture();
        fixture.payout.markProviderRequestStarted();

        fixture.service.createForCurrentUser(42L,
                new PayoutRequest(new BigDecimal("30000.00"), "970422", "0123456789"), "key-1");

        verify(fixture.provider, never()).createPayout(any());
    }

    @Test
    void invalidAmountIsRejectedBeforeReservation() {
        Fixture fixture = fixture();

        assertThrows(IllegalArgumentException.class, () -> fixture.service.createForCurrentUser(42L,
                new PayoutRequest(new BigDecimal("999.00"), "970422", "0123456789"), "key-1"));
        verify(fixture.persistence, never()).reserve(any(), any(), any(), any(), any(), any(), any());
    }

    private Fixture fixture() {
        PayoutPersistenceService persistence = mock(PayoutPersistenceService.class);
        PayoutRepository payouts = mock(PayoutRepository.class);
        PayoutProvider provider = mock(PayoutProvider.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);
        PayoutDestinationCryptoService crypto = new PayoutDestinationCryptoService(
                java.util.Base64.getEncoder().encodeToString(new byte[32]));
        AccountEntity account = new AccountEntity("ACC-PAYOUT", "Payout Owner");
        PayoutEntity payout = new PayoutEntity(account, new BigDecimal("30000.00"), "PAYOUT-1", "970422",
                "****6789", hash("0123456789"), crypto.encrypt("0123456789"), "key-1",
                new JournalEntity("PAYOUT-RESERVE-1"));
        setField(payout, "id", 1L);
        when(idempotency.normalizeKey("key-1")).thenReturn("key-1");
        when(persistence.reserve(any(), any(), any(), any(), any(), any(), any())).thenReturn(payout);
        when(persistence.markProviderRequestStarted(1L)).thenReturn(payout);
        when(persistence.attachProviderReference(1L, "provider-1")).thenReturn(payout);
        when(provider.createPayout(any())).thenReturn(
                new ProviderPayoutResult(PayoutProviderType.PAYOS, "provider-1", PayoutStatus.PENDING));
        return new Fixture(new PayoutService(persistence, payouts, provider, idempotency, crypto), persistence,
                provider, payout);
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private record Fixture(PayoutService service, PayoutPersistenceService persistence,
            PayoutProvider provider, PayoutEntity payout) {
    }
}
