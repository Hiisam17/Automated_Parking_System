package com.ktpm.parking.infrastructure.security;

import com.ktpm.parking.api.dto.LoginRequest;
import com.ktpm.parking.api.dto.LoginResponse;
import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AdminAccountRepository adminAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private SecurityProperties securityProperties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties(
                "test-gate-api-key-16-chars",
                "test-jwt-secret-with-at-least-32-characters-length",
                3600
        );
        authService = new AuthService(adminAccountRepository, passwordEncoder, jwtEncoder, securityProperties);
    }

    @Test
    void shouldLoginSuccessfullyAndReturnToken() {
        var admin = new AdminAccount(1L, "admin", "$2a$10$hashedPassword", true);
        when(adminAccountRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret123", "$2a$10$hashedPassword")).thenReturn(true);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mocked.jwt.token");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        LoginResponse response = authService.login(new LoginRequest("admin", "secret123"));

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(adminAccountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown", "secret123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
    }

    @Test
    void shouldThrowExceptionWhenUserDisabled() {
        var disabledAdmin = new AdminAccount(1L, "admin", "$2a$10$hashedPassword", false);
        when(adminAccountRepository.findByUsername("admin")).thenReturn(Optional.of(disabledAdmin));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "secret123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
    }

    @Test
    void shouldThrowExceptionWhenPasswordMismatch() {
        var admin = new AdminAccount(1L, "admin", "$2a$10$hashedPassword", true);
        when(adminAccountRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrongPassword")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
    }
}
