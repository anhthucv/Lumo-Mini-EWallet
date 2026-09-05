package com.chethu.paymentledgerservice.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.dto.TopUpRequest;
import com.chethu.paymentledgerservice.dto.TopUpResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.TopUpPaymentNotFoundException;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutRequest;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;

@Service
public class TopUpService {
    private static final Logger log = LoggerFactory.getLogger(TopUpService.class);
    private static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("1000.00");
    private static final String CURRENCY = "VND";

    private final AccountRepository accountRepository;
    private final TopUpPaymentRepository paymentRepository;
    private final TopUpPaymentPersistenceService persistenceService;
    private final IdempotencyService idempotencyService;
    private final PaymentProvider paymentProvider;

    public TopUpService(AccountRepository accountRepository, TopUpPaymentRepository paymentRepository,
            TopUpPaymentPersistenceService persistenceService, IdempotencyService idempotencyService,
            PaymentProvider paymentProvider) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.persistenceService = persistenceService;
        this.idempotencyService = idempotencyService;
        this.paymentProvider = paymentProvider;
    }

    public TopUpResponse createForCurrentUser(Long userId, TopUpRequest request, String rawIdempotencyKey) {
        validateRequest(request);
        String idempotencyKey = idempotencyService.normalizeKey(rawIdempotencyKey);
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        TopUpPaymentEntity payment = idempotencyKey == null ? null
                : paymentRepository.findByAccountAndIdempotencyKey(account, idempotencyKey).orElse(null);
        if (payment != null) {
            if (payment.getAmount().compareTo(request.amount()) != 0) {
                throw new com.chethu.paymentledgerservice.exception.IdempotencyConflictException();
            }
            if (hasCheckout(payment)) {
                return TopUpResponse.from(payment);
            }
        } else {
            payment = persistenceService.reserve(account, request.amount(), idempotencyKey);
        }

        PaymentCheckoutRequest checkoutRequest = new PaymentCheckoutRequest(
                payment.getMerchantOrderCode(), request.amount(), CURRENCY,
                "LUMO " + payment.getMerchantOrderCode());
        PaymentCheckoutResult checkout = paymentProvider.createCheckout(checkoutRequest);
        TopUpPaymentEntity attachedPayment = persistenceService.attachCheckout(payment.getId(), checkout);
        log.info("top-up checkout created userId={} topUpId={} orderCode={} amount={}", userId,
                attachedPayment.getId(), attachedPayment.getMerchantOrderCode(), attachedPayment.getAmount());
        return TopUpResponse.from(attachedPayment);
    }

    public TopUpResponse getForCurrentUser(Long userId, Long topUpId) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        return TopUpResponse.from(paymentRepository.findByIdAndAccount(topUpId, account)
                .orElseThrow(() -> new TopUpPaymentNotFoundException(topUpId)));
    }

    private boolean hasCheckout(TopUpPaymentEntity payment) {
        return payment.getStatus() == TopUpPaymentStatus.PENDING
                && payment.getProvider() != null && payment.getCheckoutUrl() != null;
    }

    private void validateRequest(TopUpRequest request) {
        if (request == null || request.amount() == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (request.amount().compareTo(MINIMUM_AMOUNT) < 0) {
            throw new IllegalArgumentException("Top-up amount must be at least 1,000 VND");
        }
    }
}
