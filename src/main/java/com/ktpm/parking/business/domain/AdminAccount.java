package com.ktpm.parking.business.domain;

public record AdminAccount(
        Long id,
        String username,
        String passwordHash,
        boolean enabled
) {
}
