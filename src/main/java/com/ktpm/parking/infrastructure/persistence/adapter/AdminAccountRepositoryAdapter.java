package com.ktpm.parking.infrastructure.persistence.adapter;

import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import com.ktpm.parking.infrastructure.persistence.mapper.AdminAccountMapper;
import com.ktpm.parking.infrastructure.persistence.springdata.AdminAccountJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AdminAccountRepositoryAdapter implements AdminAccountRepository {

    private final AdminAccountJpaRepository jpaRepository;

    public AdminAccountRepositoryAdapter(AdminAccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AdminAccount> findByUsername(String username) {
        return jpaRepository.findByUsernameIgnoreCase(username).map(AdminAccountMapper::toDomain);
    }

    @Override
    public AdminAccount save(AdminAccount adminAccount) {
        return AdminAccountMapper.toDomain(jpaRepository.save(AdminAccountMapper.toEntity(adminAccount)));
    }
}
