package com.chethu.paymentledgerservice.repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import com.chethu.paymentledgerservice.entity.AuditLogEntity;
public interface AuditLogRepository extends Repository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {
    <S extends AuditLogEntity> S save(S entity);
    Optional<AuditLogEntity> findById(Long id);
}
