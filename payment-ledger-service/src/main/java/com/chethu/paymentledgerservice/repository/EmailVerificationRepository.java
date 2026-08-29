package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.domain.EmailVerificationStatus;
import com.chethu.paymentledgerservice.entity.EmailVerificationEntity;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, Long> {
    Optional<EmailVerificationEntity> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<EmailVerificationEntity> findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(String email,
            EmailVerificationStatus status);
}
