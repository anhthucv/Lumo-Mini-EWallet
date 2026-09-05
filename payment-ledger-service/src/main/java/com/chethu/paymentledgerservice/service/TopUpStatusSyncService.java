package com.chethu.paymentledgerservice.service;

import org.springframework.stereotype.Service;

import com.chethu.paymentledgerservice.domain.TopUpPaymentStatus;
import com.chethu.paymentledgerservice.dto.TopUpResponse;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.TopUpPaymentEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.TopUpPaymentNotFoundException;
import com.chethu.paymentledgerservice.payment.provider.PaymentProvider;
import com.chethu.paymentledgerservice.payment.provider.ProviderPaymentStatusResult;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TopUpPaymentRepository;

@Service
public class TopUpStatusSyncService {
    private final AccountRepository accountRepository;
    private final TopUpPaymentRepository topUpPaymentRepository;
    private final PaymentProvider paymentProvider;
    private final TopUpFinalizationService finalizationService;

    public TopUpStatusSyncService(AccountRepository accountRepository, TopUpPaymentRepository topUpPaymentRepository,
            PaymentProvider paymentProvider, TopUpFinalizationService finalizationService) {
        this.accountRepository = accountRepository;
        this.topUpPaymentRepository = topUpPaymentRepository;
        this.paymentProvider = paymentProvider;
        this.finalizationService = finalizationService;
    }

    public TopUpResponse syncForCurrentUser(Long userId, Long topUpId) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        TopUpPaymentEntity payment = topUpPaymentRepository.findByIdAndAccount(topUpId, account)
                .orElseThrow(() -> new TopUpPaymentNotFoundException(topUpId));
        if (payment.getStatus() != TopUpPaymentStatus.PENDING) {
            return TopUpResponse.from(payment);
        }
        if (payment.getMerchantOrderCode() == null) {
            throw new com.chethu.paymentledgerservice.payment.provider.PaymentProviderException(
                    "Payment status is unavailable before checkout is created.");
        }

        ProviderPaymentStatusResult providerStatus = paymentProvider.getPaymentStatus(payment.getMerchantOrderCode());
        finalizationService.applyProviderStatus(providerStatus);
        return TopUpResponse.from(topUpPaymentRepository.findByIdAndAccount(topUpId, account)
                .orElseThrow(() -> new TopUpPaymentNotFoundException(topUpId)));
    }
}
