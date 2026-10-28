package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.exception.InvalidCredentialsException;
import com.ktpm.parking.business.port.AccessTokenIssuer;
import com.ktpm.parking.business.port.PasswordVerifier;
import com.ktpm.parking.business.repository.AdminAccountRepository;

import java.util.List;
import java.util.Objects;

public final class AuthService {

    private final AdminAccountRepository adminAccounts;
    private final PasswordVerifier passwordVerifier;
    private final AccessTokenIssuer tokenIssuer;

    public AuthService(AdminAccountRepository adminAccounts,
                       PasswordVerifier passwordVerifier,
                       AccessTokenIssuer tokenIssuer) {
        this.adminAccounts = Objects.requireNonNull(adminAccounts);
        this.passwordVerifier = Objects.requireNonNull(passwordVerifier);
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer);
    }

    public AuthenticationResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        AdminAccount admin = adminAccounts.findByUsername(username)
                .filter(AdminAccount::enabled)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordVerifier.matches(password, admin.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        return tokenIssuer.issue(admin.username(), List.of("ADMIN"));
    }
}
