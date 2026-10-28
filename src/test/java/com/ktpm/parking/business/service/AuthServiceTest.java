package com.ktpm.parking.business.service;

import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.exception.InvalidCredentialsException;
import com.ktpm.parking.business.port.AccessTokenIssuer;
import com.ktpm.parking.business.port.PasswordVerifier;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AdminAccountRepository adminAccounts;

    @Mock
    private PasswordVerifier passwordVerifier;

    @Mock
    private AccessTokenIssuer tokenIssuer;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(adminAccounts, passwordVerifier, tokenIssuer);
    }

    @Test
    void issuesTokenForValidEnabledAdmin() {
        AdminAccount admin = new AdminAccount(1L, "admin", "encoded-password", true);
        AuthenticationResult expected = AuthenticationResult.bearer("signed-token", 3600);
        when(adminAccounts.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordVerifier.matches("secret", "encoded-password")).thenReturn(true);
        when(tokenIssuer.issue("admin", List.of("ADMIN"))).thenReturn(expected);

        AuthenticationResult result = authService.login("admin", "secret");

        assertThat(result).isEqualTo(expected);
        verify(tokenIssuer).issue("admin", List.of("ADMIN"));
    }

    @Test
    void rejectsUnknownAdmin() {
        when(adminAccounts.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsDisabledAdmin() {
        AdminAccount admin = new AdminAccount(1L, "admin", "encoded-password", false);
        when(adminAccounts.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login("admin", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWrongPassword() {
        AdminAccount admin = new AdminAccount(1L, "admin", "encoded-password", true);
        when(adminAccounts.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordVerifier.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
