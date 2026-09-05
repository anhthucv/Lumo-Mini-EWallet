package com.chethu.paymentledgerservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.chethu.paymentledgerservice.entity.JournalEntity;

import com.chethu.paymentledgerservice.entity.LedgerEntryEntity;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    List<LedgerEntryEntity> findByJournalOrderByIdAsc(JournalEntity journal);
}
