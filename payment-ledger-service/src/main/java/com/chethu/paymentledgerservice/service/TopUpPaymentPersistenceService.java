package com.chethu.paymentledgerservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.payment.provider.PaymentCheckoutResult;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;

@Service
public class TopUpPaymentPersistenceService {
    private final TopUpPaymentRepository repository;

    public TopUpPaymentPersistenceService(TopUpPaymentRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TopUpPaymentEntity reserve(AccountEntity account, java.math.BigDecimal amount, String idempotencyKey) {
        TopUpPaymentEntity payment = repository.saveAndFlush(new TopUpPaymentEntity(account, amount, idempotencyKey));
        payment.assignMerchantOrderCode();
        return repository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TopUpPaymentEntity attachCheckout(Long paymentId, PaymentCheckoutResult checkout) {
        TopUpPaymentEntity payment = repository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Top-up payment could not be found after checkout"));
        payment.attachCheckout(checkout);
        return repository.save(payment);
    }
}
