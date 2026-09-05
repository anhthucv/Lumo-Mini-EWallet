package com.chethu.paymentledgerservice.payment.provider.payos;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.chethu.paymentledgerservice.config.PayOsProperties;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutRequest;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderException;
import com.chethu.paymentledgerservice.payment.provider.PaymentProviderType;
import com.chethu.paymentledgerservice.payment.provider.VerifiedPaymentWebhook;
import com.chethu.paymentledgerservice.exception.InvalidPaymentWebhookException;

import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

@Component
public class PayOsPaymentProviderAdapter implements PaymentProvider {
    private static final String SUPPORTED_CURRENCY = "VND";
    private static final String CONFIGURATION_ERROR = "payOS configuration is incomplete.";
    private static final String REQUEST_ERROR = "The payment provider could not create a checkout session.";

    private final PayOsProperties properties;

    public PayOsPaymentProviderAdapter(PayOsProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentCheckoutResult createCheckout(PaymentCheckoutRequest request) {
        validateRequest(request);
        validateConfiguration();

        CreatePaymentLinkResponse response;
        try {
            CreatePaymentLinkRequest sdkRequest = toCreatePaymentLinkRequest(request);
            response = createPaymentLink(createPayOsClient(), sdkRequest);
        } catch (PayOSException ex) {
            throw new PaymentProviderException(REQUEST_ERROR, ex);
        } catch (RuntimeException ex) {
            throw new PaymentProviderException(REQUEST_ERROR, ex);
        }
        return mapResponse(request.merchantOrderCode(), response);
    }

    protected PayOS createPayOsClient() {
        return new PayOS(
                trimToEmpty(properties.getClientId()),
                trimToEmpty(properties.getApiKey()),
                trimToEmpty(properties.getChecksumKey()));
    }

    protected CreatePaymentLinkResponse createPaymentLink(PayOS client, CreatePaymentLinkRequest request) {
        return client.paymentRequests().create(request);
    }

    @Override
    public VerifiedPaymentWebhook verifyWebhook(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new InvalidPaymentWebhookException();
        }
        try {
            WebhookData data = createPayOsClient().webhooks().verify(rawPayload);
            if (data == null) {
                throw new InvalidPaymentWebhookException();
            }
            return new VerifiedPaymentWebhook(
                    PaymentProviderType.PAYOS,
                    data.getOrderCode(),
                    data.getAmount() == null ? null : BigDecimal.valueOf(data.getAmount()),
                    data.getCurrency(),
                    data.getPaymentLinkId(),
                    data.getReference(),
                    data.getCode(),
                    "00".equals(data.getCode()));
        } catch (InvalidPaymentWebhookException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new InvalidPaymentWebhookException();
        }
    }

    private CreatePaymentLinkRequest toCreatePaymentLinkRequest(PaymentCheckoutRequest request) {
        return CreatePaymentLinkRequest.builder()
                .orderCode(request.merchantOrderCode())
                .amount(toPayOsAmount(request.amount()))
                .description(trimToEmpty(request.description()))
                .returnUrl(trimToEmpty(properties.getReturnUrl()))
                .cancelUrl(trimToEmpty(properties.getCancelUrl()))
                .build();
    }

    private PaymentCheckoutResult mapResponse(long merchantOrderCode, CreatePaymentLinkResponse response) {
        if (response == null || isBlank(response.getPaymentLinkId()) || isBlank(response.getCheckoutUrl())) {
            throw new PaymentProviderException(REQUEST_ERROR);
        }
        return new PaymentCheckoutResult(
                PaymentProviderType.PAYOS,
                merchantOrderCode,
                response.getPaymentLinkId(),
                response.getCheckoutUrl());
    }

    private void validateRequest(PaymentCheckoutRequest request) {
        if (request == null) {
            throw new PaymentProviderException("Payment checkout request is required.");
        }
        if (request.merchantOrderCode() <= 0) {
            throw new PaymentProviderException("Payment checkout merchant order code must be positive.");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new PaymentProviderException("payOS checkout amount must be positive.");
        }
        if (!SUPPORTED_CURRENCY.equalsIgnoreCase(trimToEmpty(request.currency()))) {
            throw new PaymentProviderException("payOS supports VND only.");
        }
        if (isBlank(request.description())) {
            throw new PaymentProviderException("Payment checkout description is required.");
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getClientId())
                || isBlank(properties.getApiKey())
                || isBlank(properties.getChecksumKey())
                || isBlank(properties.getReturnUrl())
                || isBlank(properties.getCancelUrl())) {
            throw new PaymentProviderException(CONFIGURATION_ERROR);
        }
    }

    private long toPayOsAmount(BigDecimal amount) {
        try {
            BigDecimal normalized = amount.stripTrailingZeros();
            if (normalized.scale() > 0) {
                throw new PaymentProviderException("payOS checkout amount must be representable in VND.");
            }
            return normalized.longValueExact();
        } catch (ArithmeticException ex) {
            throw new PaymentProviderException("payOS checkout amount must be representable in VND.", ex);
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
