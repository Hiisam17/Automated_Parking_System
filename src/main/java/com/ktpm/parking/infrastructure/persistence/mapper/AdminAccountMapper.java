package com.ktpm.parking.infrastructure.persistence.mapper;

import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.infrastructure.persistence.entity.AdminAccountEntity;

public final class AdminAccountMapper {

    private AdminAccountMapper() {
    }

    public static AdminAccount toDomain(AdminAccountEntity entity) {
        return new AdminAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.isEnabled()
        );
    }

    public static AdminAccountEntity toEntity(AdminAccount domain) {
        return new AdminAccountEntity(
                domain.id(),
                domain.username(),
                domain.passwordHash(),
                domain.enabled()
        );
    }
}
