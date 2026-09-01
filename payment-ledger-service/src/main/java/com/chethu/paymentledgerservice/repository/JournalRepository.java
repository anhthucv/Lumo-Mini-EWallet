package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.JournalEntity;

public interface JournalRepository extends JpaRepository<JournalEntity, Long> {
    Optional<JournalEntity> findByReference(String reference);
}
