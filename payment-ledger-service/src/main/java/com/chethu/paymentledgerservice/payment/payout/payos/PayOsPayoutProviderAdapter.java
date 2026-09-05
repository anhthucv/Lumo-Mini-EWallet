package com.chethu.paymentledgerservice.payment.payout.payos;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.chethu.paymentledgerservice.config.PayOsPayoutProperties;
import com.chethu.paymentledgerservice.domain.PayoutProviderType;
import com.chethu.paymentledgerservice.domain.PayoutStatus;
import com.chethu.paymentledgerservice.payment.payout.PayoutProvider;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutRequest;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutResult;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutLookupResult;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutStatus;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PayOsPayoutProviderAdapter implements PayoutProvider {
    private final PayOsPayoutProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PayOsPayoutProviderAdapter(PayOsPayoutProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    PayOsPayoutProviderAdapter(PayOsPayoutProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public ProviderPayoutResult createPayout(ProviderPayoutRequest request) {
        validateRequest(request);
        validateConfiguration();
        try {
            Map<String, Object> body = new TreeMap<>();
            body.put("amount", toWholeAmount(request.amount()));
            body.put("description", request.description());
            body.put("referenceId", request.merchantReference());
            body.put("toAccountNumber", request.destinationAccountNumber());
            body.put("toBin", request.destinationBankIdentifier());
            String json = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(trimToEmpty(properties.getBaseUrl()) + "/v1/payouts"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", properties.getClientId().trim())
                    .header("x-api-key", properties.getApiKey().trim())
                    .header("x-idempotency-key", request.idempotencyKey())
                    .header("x-signature", signature(body))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PaymentProviderException("The payout provider could not create the payout.");
            }
            return mapResponse(response.body(), request.merchantReference());
        } catch (PaymentProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentProviderException("The payout provider could not create the payout.", ex);
        }
    }

    @Override
    public Optional<ProviderPayoutLookupResult> findByMerchantReference(String merchantReference) {
        if (merchantReference == null || merchantReference.isBlank()) {
            throw new PaymentProviderException("Payout merchant reference is required.");
        }
        validateConfiguration();
        try {
            URI uri = URI.create(trimToEmpty(properties.getBaseUrl()) + "/v1/payouts?referenceId="
                    + java.net.URLEncoder.encode(merchantReference, StandardCharsets.UTF_8).replace("+", "%20"));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("x-client-id", properties.getClientId().trim())
                    .header("x-api-key", properties.getApiKey().trim())
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PaymentProviderException("The payout provider could not look up the payout.");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root == null || !"00".equals(root.path("code").asText())) {
                throw new PaymentProviderException("The payout provider rejected the payout lookup.");
            }
            JsonNode payouts = root.path("data").path("payouts");
            if (!payouts.isArray()) return Optional.empty();
            for (JsonNode payout : payouts) {
                if (merchantReference.equals(payout.path("referenceId").asText())) {
                    return Optional.of(new ProviderPayoutLookupResult(PayoutProviderType.PAYOS,
                            merchantReference, payout.path("id").asText(null), mapLookupStatus(
                                    payout.path("approvalState").asText())));
                }
            }
            return Optional.empty();
        } catch (PaymentProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentProviderException("The payout provider could not look up the payout.", ex);
        }
    }

    private ProviderPayoutStatus mapLookupStatus(String status) {
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "SUCCEEDED", "COMPLETED" -> ProviderPayoutStatus.SUCCEEDED;
            case "FAILED", "CANCELLED" -> ProviderPayoutStatus.FAILED;
            case "PROCESSING" -> ProviderPayoutStatus.PROCESSING;
            default -> ProviderPayoutStatus.UNKNOWN;
        };
    }

    private ProviderPayoutResult mapResponse(String rawBody, String merchantReference) throws Exception {
        JsonNode root = objectMapper.readTree(rawBody);
        if (root == null || !"00".equals(root.path("code").asText())) {
            throw new PaymentProviderException("The payout provider rejected the payout request.");
        }
        JsonNode data = root.path("data");
        String providerReference = data.path("id").asText(null);
        if (providerReference == null || providerReference.isBlank()
                || !merchantReference.equals(data.path("referenceId").asText())) {
            throw new PaymentProviderException("The payout provider returned an invalid payout reference.");
        }
        return new ProviderPayoutResult(PayoutProviderType.PAYOS, providerReference, PayoutStatus.PENDING);
    }

    private String signature(Map<String, Object> data) throws Exception {
        String canonical = data.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .reduce((first, second) -> first + "&" + second).orElse("");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getChecksumKey().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private long toWholeAmount(BigDecimal amount) {
        try {
            return amount.stripTrailingZeros().longValueExact();
        } catch (ArithmeticException ex) {
            throw new PaymentProviderException("Payout amount must be representable in VND.", ex);
        }
    }

    private void validateRequest(ProviderPayoutRequest request) {
        if (request == null || isBlank(request.merchantReference()) || request.amount() == null
                || request.amount().signum() <= 0 || isBlank(request.currency())
                || !"VND".equalsIgnoreCase(request.currency()) || isBlank(request.description())
                || isBlank(request.destinationBankIdentifier()) || isBlank(request.destinationAccountNumber())
                || isBlank(request.idempotencyKey())) {
            throw new PaymentProviderException("Payout request is invalid.");
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getClientId()) || isBlank(properties.getApiKey())
                || isBlank(properties.getChecksumKey()) || isBlank(properties.getBaseUrl())) {
            throw new PaymentProviderException("payOS payout configuration is incomplete.");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
