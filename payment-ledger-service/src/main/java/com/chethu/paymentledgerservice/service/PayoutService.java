package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.PayoutStatus;
import com.chethu.paymentledgerservice.domain.PayoutProviderType;
import com.chethu.paymentledgerservice.dto.PayoutRequest;
import com.chethu.paymentledgerservice.dto.PayoutResponse;
import com.chethu.paymentledgerservice.entity.PayoutEntity;
import com.chethu.paymentledgerservice.payment.payout.PayoutProvider;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutRequest;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutResult;
import com.chethu.paymentledgerservice.repository.PayoutRepository;

@Service
public class PayoutService {
    private final PayoutPersistenceService persistenceService;
    private final PayoutRepository payoutRepository;
    private final PayoutProvider payoutProvider;
    private final IdempotencyService idempotencyService;
    private final PayoutDestinationCryptoService destinationCryptoService;

    public PayoutService(PayoutPersistenceService persistenceService, PayoutRepository payoutRepository,
            PayoutProvider payoutProvider, IdempotencyService idempotencyService,
            PayoutDestinationCryptoService destinationCryptoService) {
        this.persistenceService = persistenceService;
        this.payoutRepository = payoutRepository;
        this.payoutProvider = payoutProvider;
        this.idempotencyService = idempotencyService;
        this.destinationCryptoService = destinationCryptoService;
    }

    public PayoutResponse createForCurrentUser(Long userId, PayoutRequest request, String rawIdempotencyKey) {
        validateRequest(request);
        String idempotencyKey = idempotencyService.normalizeKey(rawIdempotencyKey);
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency-Key is required for payout requests");
        }
        String accountHash = hash(request.destinationAccountNumber());
        String accountSummary = mask(request.destinationAccountNumber());
        String accountEncrypted = destinationCryptoService.encrypt(request.destinationAccountNumber());
        PayoutEntity payout = persistenceService.reserve(userId, request.amount(),
                request.destinationBankIdentifier(), accountSummary, accountHash, accountEncrypted, idempotencyKey);
        if (payout.getProviderReference() != null || payout.isProviderRequestStarted()) {
            return PayoutResponse.from(payout);
        }
        persistenceService.markProviderRequestStarted(payout.getId());
        ProviderPayoutResult result = payoutProvider.createPayout(new ProviderPayoutRequest(
                payout.getMerchantReference(), payout.getAmount(), payout.getCurrency(),
                "LUMO payout " + payout.getMerchantReference(), payout.getDestinationBankIdentifier(),
                destinationCryptoService.decrypt(payout.getDestinationAccountEncrypted()), payout.getMerchantReference()));
        if (result == null || result.provider() != PayoutProviderType.PAYOS
                || result.providerReference() == null || result.providerReference().isBlank()
                || result.status() != PayoutStatus.PENDING) {
            throw new com.chethu.paymentledgerservice.payment.provider.PaymentProviderException(
                    "The payout provider returned an invalid payout response.");
        }
        return PayoutResponse.from(persistenceService.attachProviderReference(payout.getId(), result.providerReference()));
    }

    private void validateRequest(PayoutRequest request) {
        if (request == null || request.amount() == null || request.amount().compareTo(new BigDecimal("1000.00")) < 0
                || request.amount().stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Payout amount must be at least 1,000 whole VND");
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to secure payout destination", ex);
        }
    }

    private String mask(String value) {
        return "****" + value.substring(Math.max(0, value.length() - 4));
    }
}
