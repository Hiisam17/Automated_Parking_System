package com.ktpm.parking.business.repository;

import com.ktpm.parking.business.domain.AdminAccount;

import java.util.Optional;

public interface AdminAccountRepository {

    Optional<AdminAccount> findByUsername(String username);

    AdminAccount save(AdminAccount adminAccount);
}
