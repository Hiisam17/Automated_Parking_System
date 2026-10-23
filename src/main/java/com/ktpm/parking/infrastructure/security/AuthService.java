package com.ktpm.parking.infrastructure.security;

import com.ktpm.parking.api.dto.LoginRequest;
import com.ktpm.parking.api.dto.LoginResponse;
import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;

    public AuthService(
            AdminAccountRepository adminAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            SecurityProperties securityProperties
    ) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    public LoginResponse login(LoginRequest request) {
        AdminAccount admin = adminAccountRepository.findByUsername(request.username())
                .filter(AdminAccount::enabled)
                .orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng."));

        if (!passwordEncoder.matches(request.password(), admin.passwordHash())) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        Instant now = Instant.now();
        long expiresIn = securityProperties.jwtExpirationSeconds();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("automated-parking-system")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .subject(admin.username())
                .claim("roles", List.of("ADMIN"))
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return LoginResponse.of(token, expiresIn);
    }
}
