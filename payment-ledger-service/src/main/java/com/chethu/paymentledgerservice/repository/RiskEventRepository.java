package com.chethu.paymentledgerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.RiskEventEntity;

public interface RiskEventRepository extends JpaRepository<RiskEventEntity, Long> {
}
