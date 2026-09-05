package com.chethu.paymentledgerservice.payment.payout.payos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.config.PayOsPayoutProperties;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutLookupResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class PayOsPayoutProviderAdapterTest {
    @Test
    void lookupMapsPayoutFoundByMerchantReference() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = response("{\"code\":\"00\",\"data\":{\"payouts\":[{"
                + "\"id\":\"payout-1\",\"referenceId\":\"PAYOUT-1\",\"approvalState\":\"PROCESSING\"}]}}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        Optional<ProviderPayoutLookupResult> result = adapter(client).findByMerchantReference("PAYOUT-1");

        assertTrue(result.isPresent());
        assertEquals("payout-1", result.get().providerReference());
        assertEquals("PAYOUT-1", result.get().merchantReference());
    }

    @Test
    void lookupReturnsEmptyWhenProviderHasNoMatchingPayout() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = response("{\"code\":\"00\",\"data\":{\"payouts\":[]}}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        assertTrue(adapter(client).findByMerchantReference("PAYOUT-MISSING").isEmpty());
    }

    @Test
    void lookupUsesReferenceIdQueryParameter() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = response("{\"code\":\"00\",\"data\":{\"payouts\":[]}}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            assertEquals("https://payos.test/v1/payouts?referenceId=PAYOUT-%201", request.uri().toString());
            return response;
        });

        adapter(client).findByMerchantReference("PAYOUT- 1");
    }

    private PayOsPayoutProviderAdapter adapter(HttpClient client) {
        PayOsPayoutProperties properties = new PayOsPayoutProperties();
        properties.setClientId("client");
        properties.setApiKey("api");
        properties.setChecksumKey("checksum");
        properties.setBaseUrl("https://payos.test");
        return new PayOsPayoutProviderAdapter(properties, new ObjectMapper(), client);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }
}
