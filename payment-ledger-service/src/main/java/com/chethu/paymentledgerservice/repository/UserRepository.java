package com.chethu.paymentledgerservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chethu.paymentledgerservice.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
