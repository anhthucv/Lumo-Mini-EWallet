package com.chethu.paymentledgerservice.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chethu.paymentledgerservice.entity.NotificationEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByUserOrderByCreatedAtDescIdDesc(UserEntity user, Pageable pageable);

    long countByUserAndReadAtIsNull(UserEntity user);

    Optional<NotificationEntity> findByIdAndUser(Long id, UserEntity user);

    @Modifying
    @Query("""
            update NotificationEntity n
               set n.readAt = :readAt
             where n.user = :user
               and n.readAt is null
            """)
    int markAllUnreadAsRead(@Param("user") UserEntity user, @Param("readAt") LocalDateTime readAt);
}
