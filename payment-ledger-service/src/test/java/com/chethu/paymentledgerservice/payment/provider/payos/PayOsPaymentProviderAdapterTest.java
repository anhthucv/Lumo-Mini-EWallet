package com.chethu.paymentledgerservice.payment.provider.payos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.config.PayOsProperties;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutRequest;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderException;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

class PayOsPaymentProviderAdapterTest {

    @Test
    void adapterImplementsPaymentProvider() {
        assertTrue(PaymentProvider.class.isAssignableFrom(PayOsPaymentProviderAdapter.class));
    }

    @Test
    void createCheckoutMapsRequestAndResponseFields() {
        CreatePaymentLinkResponse response = mock(CreatePaymentLinkResponse.class);
        when(response.getPaymentLinkId()).thenReturn("plink_123");
        when(response.getCheckoutUrl()).thenReturn("https://payos.example/checkout");

        RecordingAdapter adapter = new RecordingAdapter(properties(), response);
        PaymentCheckoutResult result = adapter.createCheckout(new PaymentCheckoutRequest(
                987654321L,
                new BigDecimal("100000.00"),
                "vnd",
                "Top up wallet"));

        assertEquals(PaymentProviderType.PAYOS, result.provider());
        assertEquals(987654321L, result.merchantOrderCode());
        assertEquals("plink_123", result.providerReference());
        assertEquals("https://payos.example/checkout", result.checkoutUrl());

        CreatePaymentLinkRequest sdkRequest = adapter.capturedRequest;
        assertNotNull(sdkRequest);
        assertEquals(987654321L, sdkRequest.getOrderCode());
        assertEquals(100000L, sdkRequest.getAmount());
        assertEquals("Top up wallet", sdkRequest.getDescription());
        assertEquals("http://localhost:5173/payment-result", sdkRequest.getReturnUrl());
        assertEquals("http://localhost:5173/payment-result", sdkRequest.getCancelUrl());
    }

    @Test
    void zeroAmountIsRejected() {
        RecordingAdapter adapter = new RecordingAdapter(properties(), successResponse());

        assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(new PaymentCheckoutRequest(
                1L, BigDecimal.ZERO, "VND", "Top up wallet")));
    }

    @Test
    void negativeAmountIsRejected() {
        RecordingAdapter adapter = new RecordingAdapter(properties(), successResponse());

        assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(new PaymentCheckoutRequest(
                1L, new BigDecimal("-1"), "VND", "Top up wallet")));
    }

    @Test
    void fractionalVndAmountIsRejected() {
        RecordingAdapter adapter = new RecordingAdapter(properties(), successResponse());

        assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(new PaymentCheckoutRequest(
                1L, new BigDecimal("100000.50"), "VND", "Top up wallet")));
    }

    @Test
    void unsupportedCurrencyIsRejected() {
        RecordingAdapter adapter = new RecordingAdapter(properties(), successResponse());

        assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(new PaymentCheckoutRequest(
                1L, new BigDecimal("100000"), "USD", "Top up wallet")));
    }

    @Test
    void nonPositiveMerchantOrderCodeIsRejected() {
        RecordingAdapter adapter = new RecordingAdapter(properties(), successResponse());

        assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(new PaymentCheckoutRequest(
                0L, new BigDecimal("100000"), "VND", "Top up wallet")));
    }

    @Test
    void missingClientIdFailsOnlyWhenCheckoutIsAttempted() {
        PayOsProperties properties = properties();
        properties.setClientId("");

        RecordingAdapter adapter = new RecordingAdapter(properties, successResponse());
        PaymentProviderException exception = assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(
                new PaymentCheckoutRequest(1L, new BigDecimal("100000"), "VND", "Top up wallet")));

        assertEquals("payOS configuration is incomplete.", exception.getMessage());
        assertFalse(exception.getMessage().contains("client-id"));
    }

    @Test
    void missingApiKeyFailsOnlyWhenCheckoutIsAttempted() {
        PayOsProperties properties = properties();
        properties.setApiKey("");

        RecordingAdapter adapter = new RecordingAdapter(properties, successResponse());
        PaymentProviderException exception = assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(
                new PaymentCheckoutRequest(1L, new BigDecimal("100000"), "VND", "Top up wallet")));

        assertEquals("payOS configuration is incomplete.", exception.getMessage());
        assertFalse(exception.getMessage().contains("api-key"));
    }

    @Test
    void missingChecksumKeyFailsOnlyWhenCheckoutIsAttempted() {
        PayOsProperties properties = properties();
        properties.setChecksumKey("");

        RecordingAdapter adapter = new RecordingAdapter(properties, successResponse());
        PaymentProviderException exception = assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(
                new PaymentCheckoutRequest(1L, new BigDecimal("100000"), "VND", "Top up wallet")));

        assertEquals("payOS configuration is incomplete.", exception.getMessage());
        assertFalse(exception.getMessage().contains("checksum"));
    }

    @Test
    void sdkOrProviderExceptionIsConvertedToPaymentProviderException() {
        RecordingAdapter adapter = new RecordingAdapter(properties(), null);
        adapter.failure = new RuntimeException("boom");

        PaymentProviderException exception = assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(
                new PaymentCheckoutRequest(1L, new BigDecimal("100000"), "VND", "Top up wallet")));

        assertEquals("The payment provider could not create a checkout session.", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void credentialValuesDoNotAppearInExceptionMessages() {
        PayOsProperties properties = properties();
        properties.setClientId("client-secret-123");
        properties.setApiKey("");
        properties.setChecksumKey("checksum-secret-456");

        RecordingAdapter adapter = new RecordingAdapter(properties, successResponse());
        PaymentProviderException exception = assertThrows(PaymentProviderException.class, () -> adapter.createCheckout(
                new PaymentCheckoutRequest(1L, new BigDecimal("100000"), "VND", "Top up wallet")));

        String message = exception.getMessage();
        assertFalse(message.contains("client-secret-123"));
        assertFalse(message.contains("checksum-secret-456"));
    }

    private PayOsProperties properties() {
        PayOsProperties properties = new PayOsProperties();
        properties.setClientId("client-id-123");
        properties.setApiKey("api-key-456");
        properties.setChecksumKey("checksum-key-789");
        properties.setReturnUrl("http://localhost:5173/payment-result");
        properties.setCancelUrl("http://localhost:5173/payment-result");
        return properties;
    }

    private CreatePaymentLinkResponse successResponse() {
        CreatePaymentLinkResponse response = mock(CreatePaymentLinkResponse.class);
        when(response.getPaymentLinkId()).thenReturn("plink_123");
        when(response.getCheckoutUrl()).thenReturn("https://payos.example/checkout");
        return response;
    }

    private static final class RecordingAdapter extends PayOsPaymentProviderAdapter {
        private final CreatePaymentLinkResponse response;
        private CreatePaymentLinkRequest capturedRequest;
        private RuntimeException failure;

        private RecordingAdapter(PayOsProperties properties, CreatePaymentLinkResponse response) {
            super(properties);
            this.response = response;
        }

        @Override
        protected PayOS createPayOsClient() {
            return null;
        }

        @Override
        protected CreatePaymentLinkResponse createPaymentLink(PayOS client, CreatePaymentLinkRequest request) {
            capturedRequest = request;
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
