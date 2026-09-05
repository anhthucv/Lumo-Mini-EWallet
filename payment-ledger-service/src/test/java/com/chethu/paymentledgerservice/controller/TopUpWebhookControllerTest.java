package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;
import com.chethu.paymentledgerservice.payment.provider.VerifiedPaymentWebhook;
import com.chethu.paymentledgerservice.service.TopUpFinalizationService;

class TopUpWebhookControllerTest {
    @Test
    void verifiesRawPayloadDelegatesFinalizationAndAcknowledges() {
        PaymentProvider provider = mock(PaymentProvider.class);
        TopUpFinalizationService finalization = mock(TopUpFinalizationService.class);
        TopUpWebhookController controller = new TopUpWebhookController(provider, finalization);
        VerifiedPaymentWebhook webhook = new VerifiedPaymentWebhook(PaymentProviderType.PAYOS, 77L,
                new BigDecimal("10000.00"), "VND", "payment-link", "reference", "00", true);
        when(provider.verifyWebhook("raw-payload")).thenReturn(webhook);

        var response = controller.receive("raw-payload");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("success", true), response.getBody());
        verify(provider).verifyWebhook(eq("raw-payload"));
        verify(finalization).finalizeVerifiedWebhook(webhook);
    }
}
