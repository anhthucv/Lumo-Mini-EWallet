package com.chethu.paymentledgerservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.BeneficiaryEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;

public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, Long> {
    List<BeneficiaryEntity> findAllByOwnerOrderByCreatedAtDesc(UserEntity owner);

    Optional<BeneficiaryEntity> findByIdAndOwner(Long id, UserEntity owner);

    boolean existsByOwnerAndBeneficiaryAccount(UserEntity owner, AccountEntity beneficiaryAccount);
}
