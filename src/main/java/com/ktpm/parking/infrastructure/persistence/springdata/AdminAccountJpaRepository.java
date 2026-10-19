package com.ktpm.parking.infrastructure.persistence.springdata;

import com.ktpm.parking.infrastructure.persistence.entity.AdminAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminAccountJpaRepository extends JpaRepository<AdminAccountEntity, Long> {

    Optional<AdminAccountEntity> findByUsernameIgnoreCase(String username);
}
